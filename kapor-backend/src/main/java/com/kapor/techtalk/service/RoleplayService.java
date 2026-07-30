package com.kapor.techtalk.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.dto.RoleplayHistoryDto;
import com.kapor.techtalk.dto.RoleplayStreamEvent;
import com.kapor.techtalk.dto.StreamRoleplayTurnRequest;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import com.kapor.techtalk.repository.TechTalkScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleplayService {
    private final TechTalkScenarioRepository scenarioRepository;
    private final RoleplaySessionRepository sessionRepository;
    private final RoleplayPromptService promptService;
    private final RoleplayAiProvider aiProvider;
    private final RoleplayRateLimiter rateLimiter;
    private final RoleplayMetrics metrics;
    private final RoleplaySpeechService speechService;

    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> activeGenerations = new ConcurrentHashMap<>();

    @Value("${techtalk.session.stale-after-hours:12}")
    private int staleAfterHours;

    @Value("${techtalk.session.test-retention-hours:24}")
    private int testRetentionHours;

    public List<TechTalkScenario> getScenarios() {
        return scenarioRepository.findByActiveTrueOrderByOrderAsc();
    }

    public TechTalkScenario getScenario(String id) {
        return scenarioRepository.findById(id)
                .filter(TechTalkScenario::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", "id", id));
    }

    public RoleplaySession start(String userId, String scenarioId) {
        return start(userId, scenarioId, false);
    }

    public RoleplaySession start(String userId, String scenarioId, boolean testMode) {
        TechTalkScenario scenario = getScenario(scenarioId);
        if (!testMode) {
            RoleplaySession resumable = sessionRepository
                    .findFirstByUserIdAndScenarioIdAndStatusOrderByStartedAtDesc(userId, scenarioId, "active")
                    .orElse(null);
            if (resumable != null) {
                Instant staleCutoff = Instant.now().minus(Duration.ofHours(Math.max(1, staleAfterHours)));
                if (resumable.getLastActivityAt() == null || resumable.getLastActivityAt().isAfter(staleCutoff)) {
                    return resumable;
                }
                resumable.setStatus("abandoned");
                resumable.setEndedAt(Instant.now());
                resumable.setDurationSeconds(resumable.getStartedAt() == null ? 0
                        : Math.max(0, Duration.between(resumable.getStartedAt(), resumable.getEndedAt()).toSeconds()));
                sessionRepository.save(resumable);
                metrics.increment("sessions.auto_abandoned");
            }
        }
        RoleplayPromptService.PromptSnapshot prompt = promptService.resolve(scenario);
        Instant now = Instant.now();
        RoleplaySession session = RoleplaySession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .status("active")
                .testMode(testMode)
                .startedAt(now)
                .lastActivityAt(now)
                .promptSnapshot(prompt.content())
                .promptVersion(prompt.version())
                .modelName(aiProvider.modelName())
                .messages(new ArrayList<>(List.of(RoleplaySession.Message.builder()
                        .id(UUID.randomUUID().toString())
                        .role("ai")
                        .source("system")
                        .generationStatus("completed")
                        .content(openingMessage(scenario))
                        .timestamp(now)
                        .build())))
                .build();
        metrics.increment("sessions.started");
        return sessionRepository.save(session);
    }

    /** Compatibility endpoint for clients that have not moved to SSE yet. */
    public RoleplaySession send(String userId, String sessionId, String content) {
        StreamRoleplayTurnRequest request = new StreamRoleplayTurnRequest();
        request.setClientTurnId(UUID.randomUUID().toString());
        request.setContent(content);
        request.setSource("text");
        streamTurn(userId, sessionId, request).blockLast(Duration.ofSeconds(100));
        RoleplaySession result = ownedSession(userId, sessionId);
        RoleplaySession.Turn latest = result.getTurns().isEmpty() ? null : result.getTurns().get(result.getTurns().size() - 1);
        if (latest != null && "failed".equals(latest.getStatus())) {
            throw new RoleplayAiException("Không thể hoàn thành lượt TechTalk.", true);
        }
        return result;
    }

    public Flux<RoleplayStreamEvent> streamTurn(
            String userId,
            String sessionId,
            StreamRoleplayTurnRequest request
    ) {
        TurnWork work = beginTurn(userId, sessionId, request);
        if (work.replay()) return replay(work);

        RoleplayStreamEvent accepted = RoleplayStreamEvent.builder()
                .type("turn.accepted")
                .sessionId(sessionId)
                .turnId(work.turnId())
                .userMessageId(work.userMessageId())
                .build();
        AtomicReference<RoleplaySession.Evaluation> evaluation = new AtomicReference<>();
        StringBuilder reply = new StringBuilder();
        Instant generationStarted = Instant.now();

        Mono<RoleplaySession.Evaluation> evaluationMono = aiProvider
                .evaluateTurn(work.context(), request.getContent().trim())
                .onErrorResume(error -> {
                    metrics.increment("evaluation.errors");
                    log.warn("TechTalk evaluation unavailable for turn {}: {}", work.turnId(), error.getMessage());
                    return Mono.just(unavailableEvaluation());
                })
                .doOnNext(evaluation::set)
                .cache();

        Flux<RoleplayStreamEvent> tokens = aiProvider.streamReply(work.context())
                .doOnNext(reply::append)
                .index()
                .map(indexed -> {
                    if (indexed.getT1() == 0) {
                        metrics.record("time_to_first_token", Duration.between(generationStarted, Instant.now()));
                    }
                    return RoleplayStreamEvent.builder().type("token").sessionId(sessionId)
                            .turnId(work.turnId()).delta(indexed.getT2()).build();
                });

        Flux<RoleplayStreamEvent> evaluationEvent = evaluationMono.map(value ->
                RoleplayStreamEvent.builder().type("evaluation").sessionId(sessionId).turnId(work.turnId())
                        .messageId(work.userMessageId()).evaluation(value).build()).flux();

        Flux<RoleplayStreamEvent> generation = Flux.merge(tokens, evaluationEvent)
                .concatWith(Mono.fromCallable(() -> completeTurn(
                                userId, sessionId, work, evaluation.get(), reply.toString()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(message -> RoleplayStreamEvent.builder()
                                .type("message.completed").sessionId(sessionId).turnId(work.turnId())
                                .messageId(message.getId()).message(message).build()))
                .concatWith(Mono.fromSupplier(() -> RoleplayStreamEvent.builder()
                        .type("done").sessionId(sessionId).turnId(work.turnId()).build()))
                .onErrorResume(error -> Mono.fromCallable(() -> {
                            failTurn(userId, sessionId, work.turnId(), error);
                            boolean retryable = !(error instanceof RoleplayAiException aiError) || aiError.isRetryable();
                            return RoleplayStreamEvent.builder().type("error").sessionId(sessionId).turnId(work.turnId())
                                    .code("TECHTALK_AI_UNAVAILABLE")
                                    .messageText("Không thể nhận phản hồi AI. Bạn có thể thử lại lượt này.")
                                    .retryable(retryable).build();
                        }).subscribeOn(Schedulers.boundedElastic()))
                .doFinally(signal -> activeGenerations.remove(sessionId, request.getClientTurnId()));

        return Flux.concat(Mono.just(accepted), generation);
    }

    public RoleplayHintDto hint(String userId, String sessionId) {
        RoleplaySession session = ownedActiveSession(userId, sessionId);
        TechTalkScenario scenario = getScenario(session.getScenarioId());
        RoleplayHintDto result;
        try {
            result = aiProvider.hint(promptService.context(scenario, session)).block(Duration.ofSeconds(50));
        } catch (RuntimeException exception) {
            log.warn("Falling back to scenario scaffold hint for {}: {}", sessionId, exception.getMessage());
            List<String> vocabulary = effectiveVocabulary(scenario);
            result = RoleplayHintDto.builder()
                    .keywords(vocabulary.stream().limit(5).toList())
                    .sentenceStructure("[상황]이 발생하여 [영향]을 확인하고 있습니다.")
                    .politenessTip("직장에서는 -습니다/-ㅂ니다 종결형을 일관되게 사용하세요.")
                    .build();
        }
        synchronized (lock(sessionId)) {
            RoleplaySession latest = ownedActiveSession(userId, sessionId);
            latest.setHintsUsed(latest.getHintsUsed() + 1);
            latest.setLastActivityAt(Instant.now());
            sessionRepository.save(latest);
        }
        metrics.increment("hints.used");
        return result;
    }

    public RoleplaySession end(String userId, String sessionId) {
        synchronized (lock(sessionId)) {
            RoleplaySession session = ownedSession(userId, sessionId);
            if ("completed".equals(session.getStatus())) return session;
            if ("abandoned".equals(session.getStatus())) {
                throw new IllegalArgumentException("Abandoned roleplay sessions cannot be completed");
            }
            if (activeGenerations.containsKey(sessionId)) {
                throw new IllegalArgumentException("Wait for the current AI response before ending the session");
            }
            TechTalkScenario scenario = getScenario(session.getScenarioId());
            List<RoleplaySession.Evaluation> evaluations = session.getMessages().stream()
                    .map(RoleplaySession.Message::getEvaluation)
                    .filter(Objects::nonNull)
                    .filter(value -> "completed".equals(value.getStatus()))
                    .toList();
            int grammar = average(evaluations, Metric.GRAMMAR);
            int vocabulary = average(evaluations, Metric.VOCABULARY);
            int politeness = average(evaluations, Metric.POLITENESS);
            RoleplaySession.FinalEvaluation taskEvaluation;
            try {
                taskEvaluation = aiProvider.evaluateSession(promptService.context(scenario, session))
                        .block(Duration.ofSeconds(60));
            } catch (RuntimeException exception) {
                log.warn("Final TechTalk evaluator unavailable for {}: {}", sessionId, exception.getMessage());
                taskEvaluation = degradedFinalEvaluation(session, evaluations.size());
            }
            if (taskEvaluation == null) taskEvaluation = degradedFinalEvaluation(session, evaluations.size());
            taskEvaluation.setGrammar(grammar);
            taskEvaluation.setVocabulary(vocabulary);
            taskEvaluation.setPoliteness(politeness);
            taskEvaluation.setOverallScore(weightedScore(scenario, grammar, vocabulary, politeness,
                    taskEvaluation.getTaskCompletion()));
            session.setFinalEvaluation(taskEvaluation);
            session.setStatus("completed");
            session.setEndedAt(Instant.now());
            session.setLastActivityAt(session.getEndedAt());
            session.setDurationSeconds(Math.max(0, Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds()));
            RoleplaySession saved = sessionRepository.save(session);
            metrics.increment("sessions.completed");
            return saved;
        }
    }

    public RoleplaySession abandon(String userId, String sessionId) {
        synchronized (lock(sessionId)) {
            RoleplaySession session = ownedSession(userId, sessionId);
            if ("active".equals(session.getStatus())) {
                session.setStatus("abandoned");
                session.setEndedAt(Instant.now());
                session.setLastActivityAt(session.getEndedAt());
                session.setDurationSeconds(Math.max(0,
                        Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds()));
                session = sessionRepository.save(session);
                metrics.increment("sessions.abandoned");
            }
            activeGenerations.remove(sessionId);
            return session;
        }
    }

    public RoleplaySession detail(String userId, String sessionId) {
        return ownedSession(userId, sessionId);
    }

    public List<RoleplaySession> history(String userId) {
        return sessionRepository.findByUserIdAndTestModeFalseOrderByStartedAtDesc(userId);
    }

    public RoleplayHistoryDto history(String userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        List<RoleplaySession> rows = sessionRepository.findByUserIdAndTestModeFalseOrderByStartedAtDesc(
                userId, PageRequest.of(safePage, safeSize + 1));
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, safeSize));
        return RoleplayHistoryDto.builder().content(rows).page(safePage).size(safeSize).hasMore(hasMore).build();
    }

    @Scheduled(cron = "${techtalk.session.cleanup-cron:0 15 * * * *}")
    public void abandonStaleSessions() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofHours(Math.max(1, staleAfterHours)));
        for (RoleplaySession session : sessionRepository.findByStatusAndLastActivityAtBefore("active", cutoff)) {
            session.setStatus("abandoned");
            session.setEndedAt(now);
            session.setDurationSeconds(session.getStartedAt() == null ? 0
                    : Math.max(0, Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds()));
            sessionRepository.save(session);
        }
        sessionRepository.deleteAll(sessionRepository.findByTestModeTrueAndEndedAtBefore(
                now.minus(Duration.ofHours(Math.max(1, testRetentionHours)))));
    }

    private TurnWork beginTurn(String userId, String sessionId, StreamRoleplayTurnRequest request) {
        synchronized (lock(sessionId)) {
            RoleplaySession session = ownedActiveSession(userId, sessionId);
            RoleplaySession.Turn existing = session.getTurns().stream()
                    .filter(turn -> request.getClientTurnId().equals(turn.getClientTurnId()))
                    .findFirst().orElse(null);
            if (existing != null) {
                RoleplaySession.Message userMessage = message(session, existing.getUserMessageId());
                RoleplaySession.Message aiMessage = message(session, existing.getAiMessageId());
                if ("completed".equals(existing.getStatus()) || aiMessage != null
                        || activeGenerations.containsKey(sessionId)) {
                    TechTalkScenario scenario = getScenario(session.getScenarioId());
                    return new TurnWork(session, scenario, existing.getId(),
                            existing.getUserMessageId(), userMessage, aiMessage, true,
                            promptService.context(scenario, session));
                }
                if (userMessage == null || !request.getContent().trim().equals(userMessage.getContent())) {
                    throw new IllegalArgumentException("A client turn id cannot be reused with different content");
                }
                String previous = activeGenerations.putIfAbsent(sessionId, request.getClientTurnId());
                if (previous != null) {
                    throw new IllegalArgumentException("An AI response is already being generated for this session");
                }
                try {
                    int completedTurns = (int) session.getTurns().stream()
                            .filter(turn -> "completed".equals(turn.getStatus())).count();
                    rateLimiter.checkTurn(userId, sessionId, completedTurns, session.isTestMode());
                    if ("voice".equals(userMessage.getSource())) {
                        speechService.validateAsset(userId, sessionId, userMessage.getAudioId());
                    }
                    existing.setStatus("generating");
                    existing.setErrorCode(null);
                    existing.setCompletedAt(null);
                    userMessage.setGenerationStatus("completed");
                    session.setLastActivityAt(Instant.now());
                    session = sessionRepository.save(session);
                    TechTalkScenario scenario = getScenario(session.getScenarioId());
                    metrics.increment("turns.retried");
                    return new TurnWork(session, scenario, existing.getId(), existing.getUserMessageId(),
                            userMessage, null, false, promptService.context(scenario, session));
                } catch (RuntimeException exception) {
                    activeGenerations.remove(sessionId, request.getClientTurnId());
                    throw exception;
                }
            }
            String previous = activeGenerations.putIfAbsent(sessionId, request.getClientTurnId());
            if (previous != null) throw new IllegalArgumentException("An AI response is already being generated for this session");
            try {
                int completedTurns = (int) session.getTurns().stream().filter(turn -> "completed".equals(turn.getStatus())).count();
                rateLimiter.checkTurn(userId, sessionId, completedTurns, session.isTestMode());
                if ("voice".equals(request.getSource())) {
                    speechService.validateAsset(userId, sessionId, request.getAudioId());
                }
                Instant now = Instant.now();
                String turnId = UUID.randomUUID().toString();
                String userMessageId = UUID.randomUUID().toString();
                RoleplaySession.Message userMessage = RoleplaySession.Message.builder()
                        .id(userMessageId)
                        .clientTurnId(request.getClientTurnId())
                        .role("user")
                        .content(request.getContent().trim())
                        .source(request.getSource())
                        .transcript(request.getTranscript())
                        .audioId(request.getAudioId())
                        .generationStatus("completed")
                        .timestamp(now)
                        .build();
                RoleplaySession.Turn turn = RoleplaySession.Turn.builder()
                        .id(turnId).clientTurnId(request.getClientTurnId()).userMessageId(userMessageId)
                        .status("generating").createdAt(now).build();
                session.getMessages().add(userMessage);
                session.getTurns().add(turn);
                session.setLastActivityAt(now);
                session = sessionRepository.save(session);
                TechTalkScenario scenario = getScenario(session.getScenarioId());
                metrics.increment("turns.started");
                return new TurnWork(session, scenario, turnId, userMessageId, userMessage, null, false,
                        promptService.context(scenario, session));
            } catch (RuntimeException exception) {
                activeGenerations.remove(sessionId, request.getClientTurnId());
                throw exception;
            }
        }
    }

    private RoleplaySession.Message completeTurn(
            String userId,
            String sessionId,
            TurnWork work,
            RoleplaySession.Evaluation evaluation,
            String rawReply
    ) {
        String reply = rawReply == null ? "" : rawReply.trim();
        if (reply.isBlank() || !containsKorean(reply)) {
            throw new RoleplayAiException("Gemini returned an invalid Korean roleplay response.", true);
        }
        synchronized (lock(sessionId)) {
            RoleplaySession session = ownedActiveSession(userId, sessionId);
            RoleplaySession.Message userMessage = message(session, work.userMessageId());
            userMessage.setEvaluation(evaluation == null ? unavailableEvaluation() : evaluation);
            String aiMessageId = UUID.randomUUID().toString();
            RoleplaySession.Message aiMessage = RoleplaySession.Message.builder()
                    .id(aiMessageId).role("ai").content(reply).source("ai")
                    .generationStatus("completed").timestamp(Instant.now()).build();
            session.getMessages().add(aiMessage);
            RoleplaySession.Turn turn = turn(session, work.turnId());
            turn.setAiMessageId(aiMessageId);
            turn.setStatus("completed");
            turn.setCompletedAt(Instant.now());
            session.setLastActivityAt(turn.getCompletedAt());
            sessionRepository.save(session);
            metrics.increment("turns.completed");
            return aiMessage;
        }
    }

    private void failTurn(String userId, String sessionId, String turnId, Throwable error) {
        synchronized (lock(sessionId)) {
            try {
                RoleplaySession session = ownedSession(userId, sessionId);
                RoleplaySession.Turn turn = turn(session, turnId);
                turn.setStatus("failed");
                turn.setErrorCode("TECHTALK_AI_UNAVAILABLE");
                turn.setCompletedAt(Instant.now());
                RoleplaySession.Message user = message(session, turn.getUserMessageId());
                if (user != null) user.setGenerationStatus("response_failed");
                session.setLastActivityAt(Instant.now());
                sessionRepository.save(session);
                metrics.increment("turns.failed");
                log.warn("TechTalk turn {} failed: {}", turnId, error.getMessage());
            } catch (RuntimeException persistenceError) {
                log.error("Could not persist failed TechTalk turn {}", turnId, persistenceError);
            }
        }
    }

    private Flux<RoleplayStreamEvent> replay(TurnWork work) {
        List<RoleplayStreamEvent> events = new ArrayList<>();
        events.add(RoleplayStreamEvent.builder().type("turn.accepted").sessionId(work.session().getId())
                .turnId(work.turnId()).userMessageId(work.userMessageId()).build());
        if (work.userMessage() != null && work.userMessage().getEvaluation() != null) {
            events.add(RoleplayStreamEvent.builder().type("evaluation").sessionId(work.session().getId())
                    .turnId(work.turnId()).messageId(work.userMessageId())
                    .evaluation(work.userMessage().getEvaluation()).build());
        }
        if (work.aiMessage() != null) {
            events.add(RoleplayStreamEvent.builder().type("message.completed").sessionId(work.session().getId())
                    .turnId(work.turnId()).messageId(work.aiMessage().getId()).message(work.aiMessage()).build());
            events.add(RoleplayStreamEvent.builder().type("done").sessionId(work.session().getId())
                    .turnId(work.turnId()).build());
        } else {
            events.add(RoleplayStreamEvent.builder().type("error").sessionId(work.session().getId())
                    .turnId(work.turnId()).code("TURN_INCOMPLETE")
                    .messageText("Lượt trước chưa hoàn thành. Hãy gửi lại với clientTurnId mới.")
                    .retryable(true).build());
        }
        return Flux.fromIterable(events);
    }

    private RoleplaySession ownedSession(String userId, String sessionId) {
        return sessionRepository.findById(sessionId).filter(session -> userId.equals(session.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Roleplay session", "id", sessionId));
    }

    private RoleplaySession ownedActiveSession(String userId, String sessionId) {
        RoleplaySession session = ownedSession(userId, sessionId);
        if (!"active".equals(session.getStatus())) throw new IllegalArgumentException("Roleplay session is no longer active");
        return session;
    }

    private Object lock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, ignored -> new Object());
    }

    private RoleplaySession.Message message(RoleplaySession session, String id) {
        if (id == null) return null;
        return session.getMessages().stream().filter(value -> id.equals(value.getId())).findFirst().orElse(null);
    }

    private RoleplaySession.Turn turn(RoleplaySession session, String id) {
        return session.getTurns().stream().filter(value -> id.equals(value.getId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Roleplay turn", "id", id));
    }

    private String openingMessage(TechTalkScenario scenario) {
        String missionTitle = scenario.getMission() == null ? null : scenario.getMission().getTitleKo();
        String subject = missionTitle != null && !missionTitle.isBlank() ? missionTitle : scenario.getTitle();
        String name = scenario.getPersona() == null ? "" : scenario.getPersona().getName();
        return "안녕하세요. " + (name == null || name.isBlank() ? "" : name + "입니다. ")
                + subject + " 상황을 시작하겠습니다. 먼저 상황을 보고해 주시겠습니까?";
    }

    private List<String> effectiveVocabulary(TechTalkScenario scenario) {
        if (scenario.getMission() != null && scenario.getMission().getRequiredVocabulary() != null
                && !scenario.getMission().getRequiredVocabulary().isEmpty()) {
            return scenario.getMission().getRequiredVocabulary();
        }
        return scenario.getRequiredVocabulary() == null ? List.of() : scenario.getRequiredVocabulary();
    }

    private RoleplaySession.Evaluation unavailableEvaluation() {
        return RoleplaySession.Evaluation.builder().status("unavailable")
                .feedbackVi("Chưa thể chấm điểm lượt này. Hội thoại vẫn được tiếp tục.")
                .corrections(List.of()).usedRequiredVocabulary(List.of()).build();
    }

    private RoleplaySession.FinalEvaluation degradedFinalEvaluation(RoleplaySession session, int evaluatedTurns) {
        int completion = Math.min(100, evaluatedTurns * 20);
        return RoleplaySession.FinalEvaluation.builder()
                .taskCompletion(completion)
                .feedback("세션이 완료되었습니다.")
                .feedbackVi("Phiên đã hoàn thành nhưng đánh giá nhiệm vụ AI tạm thời không khả dụng.")
                .improvementAreas(List.of("Xem lại transcript và luyện lại các câu đã được sửa."))
                .build();
    }

    private int average(List<RoleplaySession.Evaluation> values, Metric metric) {
        if (values.isEmpty()) return 0;
        return (int) Math.round(values.stream().mapToInt(value -> switch (metric) {
            case GRAMMAR -> value.getGrammar();
            case VOCABULARY -> value.getVocabulary();
            case POLITENESS -> value.getPoliteness();
        }).average().orElse(0));
    }

    private int weightedScore(TechTalkScenario scenario, int grammar, int vocabulary, int politeness, int completion) {
        TechTalkScenario.EvaluationCriteria criteria = scenario.getEvaluationCriteria();
        double grammarWeight = criteria == null ? .30 : criteria.getGrammarWeight();
        double vocabularyWeight = criteria == null ? .30 : criteria.getVocabularyWeight();
        double politenessWeight = criteria == null ? .25 : criteria.getPolitenessWeight();
        double completionWeight = criteria == null ? .15 : criteria.getTaskCompletionWeight();
        double total = grammarWeight + vocabularyWeight + politenessWeight + completionWeight;
        if (total <= 0) total = 1;
        return (int) Math.round((grammar * grammarWeight + vocabulary * vocabularyWeight
                + politeness * politenessWeight + completion * completionWeight) / total);
    }

    private boolean containsKorean(String text) {
        return text.codePoints().anyMatch(code -> code >= 0xAC00 && code <= 0xD7A3);
    }

    private enum Metric { GRAMMAR, VOCABULARY, POLITENESS }

    private record TurnWork(
            RoleplaySession session,
            TechTalkScenario scenario,
            String turnId,
            String userMessageId,
            RoleplaySession.Message userMessage,
            RoleplaySession.Message aiMessage,
            boolean replay,
            RoleplayContext context
    ) { }
}

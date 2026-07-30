package com.kapor.techtalk.service;

import com.kapor.techtalk.dto.RoleplayStreamEvent;
import com.kapor.techtalk.dto.StreamRoleplayTurnRequest;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import com.kapor.techtalk.repository.TechTalkScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleplayServiceTest {
    private final TechTalkScenarioRepository scenarioRepository = mock(TechTalkScenarioRepository.class);
    private final RoleplaySessionRepository sessionRepository = mock(RoleplaySessionRepository.class);
    private final RoleplayPromptService promptService = mock(RoleplayPromptService.class);
    private final RoleplayAiProvider aiProvider = mock(RoleplayAiProvider.class);
    private final RoleplayRateLimiter rateLimiter = mock(RoleplayRateLimiter.class);
    private final RoleplayMetrics metrics = mock(RoleplayMetrics.class);
    private final RoleplaySpeechService speechService = mock(RoleplaySpeechService.class);
    private final AtomicReference<RoleplaySession> storedSession = new AtomicReference<>();
    private RoleplayService service;
    private TechTalkScenario scenario;

    @BeforeEach
    void setUp() {
        service = new RoleplayService(scenarioRepository, sessionRepository, promptService, aiProvider,
                rateLimiter, metrics, speechService);
        scenario = TechTalkScenario.builder()
                .id("scenario-1")
                .title("장애 보고")
                .titleVi("Báo cáo sự cố")
                .difficulty("intermediate")
                .active(true)
                .persona(TechTalkScenario.Persona.builder().name("김민수").role("Tech Lead").company("Kapor").build())
                .mission(TechTalkScenario.Mission.builder().titleKo("장애 보고").build())
                .evaluationCriteria(TechTalkScenario.EvaluationCriteria.builder().build())
                .build();
        when(scenarioRepository.findById("scenario-1")).thenReturn(Optional.of(scenario));
        when(sessionRepository.findFirstByUserIdAndScenarioIdAndStatusOrderByStartedAtDesc(
                anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(RoleplaySession.class))).thenAnswer(invocation -> {
            RoleplaySession session = invocation.getArgument(0);
            if (session.getId() == null) session.setId("session-1");
            storedSession.set(session);
            return session;
        });
        when(sessionRepository.findById("session-1")).thenAnswer(ignored -> Optional.ofNullable(storedSession.get()));
        when(promptService.resolve(scenario)).thenReturn(
                new RoleplayPromptService.PromptSnapshot("system prompt", "prompt:v1"));
        when(promptService.context(any(), any())).thenAnswer(invocation -> {
            RoleplaySession session = invocation.getArgument(1);
            return new RoleplayContext(scenario, session, session.getPromptSnapshot(),
                    session.getPromptVersion(), List.copyOf(session.getMessages()));
        });
        when(aiProvider.modelName()).thenReturn("gemini-test");
        doNothing().when(rateLimiter).checkTurn(anyString(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    void streamsPersistsAndIdempotentlyReplaysATurn() {
        RoleplaySession.Evaluation evaluation = RoleplaySession.Evaluation.builder()
                .grammar(90).vocabulary(80).politeness(95).status("completed").build();
        when(aiProvider.evaluateTurn(any(), anyString())).thenReturn(Mono.just(evaluation));
        when(aiProvider.streamReply(any())).thenReturn(Flux.just("확인했습니다. ", "롤백 계획을 설명해 주세요."));
        RoleplaySession started = service.start("user-1", "scenario-1");
        StreamRoleplayTurnRequest request = request("turn-1");

        List<RoleplayStreamEvent> first = service.streamTurn("user-1", started.getId(), request)
                .collectList().block();
        List<RoleplayStreamEvent> replay = service.streamTurn("user-1", started.getId(), request)
                .collectList().block();

        assertThat(first).extracting(RoleplayStreamEvent::getType)
                .contains("turn.accepted", "token", "evaluation", "message.completed", "done");
        assertThat(replay).extracting(RoleplayStreamEvent::getType)
                .containsExactly("turn.accepted", "evaluation", "message.completed", "done");
        assertThat(storedSession.get().getTurns()).singleElement()
                .satisfies(turn -> assertThat(turn.getStatus()).isEqualTo("completed"));
        assertThat(storedSession.get().getMessages()).hasSize(3);
        assertThat(storedSession.get().getMessages().get(1).getEvaluation()).isSameAs(evaluation);
        verify(aiProvider, times(1)).streamReply(any());
        verify(rateLimiter, times(1)).checkTurn(anyString(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    void releasesGenerationGuardWhenQuotaCheckRejectsTheTurn() {
        service.start("user-1", "scenario-1");
        doThrow(new RoleplayRateLimitException("limited", 30))
                .doNothing()
                .when(rateLimiter).checkTurn(anyString(), anyString(), anyInt(), anyBoolean());
        when(aiProvider.evaluateTurn(any(), anyString())).thenReturn(Mono.just(
                RoleplaySession.Evaluation.builder().status("completed").build()));
        when(aiProvider.streamReply(any())).thenReturn(Flux.just("다시 시도할 수 있습니다."));

        assertThatThrownBy(() -> service.streamTurn("user-1", "session-1", request("turn-rejected")))
                .isInstanceOf(RoleplayRateLimitException.class);

        List<RoleplayStreamEvent> retry = service.streamTurn(
                "user-1", "session-1", request("turn-accepted")).collectList().block();
        assertThat(retry).extracting(RoleplayStreamEvent::getType).contains("message.completed");
    }

    @Test
    void retriesAFailedGenerationWithTheSameIdWithoutDuplicatingTheUserMessage() {
        service.start("user-1", "scenario-1");
        when(aiProvider.evaluateTurn(any(), anyString())).thenReturn(Mono.just(
                RoleplaySession.Evaluation.builder().status("completed").build()));
        when(aiProvider.streamReply(any()))
                .thenReturn(Flux.error(new RoleplayAiException("temporary", true)))
                .thenReturn(Flux.just("재시도 응답입니다."));
        StreamRoleplayTurnRequest request = request("stable-client-turn");

        List<RoleplayStreamEvent> failed = service.streamTurn(
                "user-1", "session-1", request).collectList().block();
        List<RoleplayStreamEvent> retried = service.streamTurn(
                "user-1", "session-1", request).collectList().block();

        assertThat(failed).extracting(RoleplayStreamEvent::getType).contains("error");
        assertThat(retried).extracting(RoleplayStreamEvent::getType).contains("message.completed", "done");
        assertThat(storedSession.get().getTurns()).hasSize(1);
        assertThat(storedSession.get().getMessages().stream()
                .filter(message -> "user".equals(message.getRole()))).hasSize(1);
    }

    @Test
    void abandonsAStaleSessionBeforeStartingANewOne() {
        ReflectionTestUtils.setField(service, "staleAfterHours", 12);
        RoleplaySession stale = RoleplaySession.builder()
                .id("stale-session")
                .userId("user-1")
                .scenarioId("scenario-1")
                .status("active")
                .startedAt(Instant.now().minusSeconds(86_400))
                .lastActivityAt(Instant.now().minusSeconds(86_400))
                .build();
        when(sessionRepository.findFirstByUserIdAndScenarioIdAndStatusOrderByStartedAtDesc(
                "user-1", "scenario-1", "active")).thenReturn(Optional.of(stale));

        RoleplaySession started = service.start("user-1", "scenario-1");

        assertThat(stale.getStatus()).isEqualTo("abandoned");
        assertThat(stale.getEndedAt()).isNotNull();
        assertThat(started.getId()).isEqualTo("session-1");
        assertThat(started.getStatus()).isEqualTo("active");
    }

    private StreamRoleplayTurnRequest request(String id) {
        StreamRoleplayTurnRequest request = new StreamRoleplayTurnRequest();
        request.setClientTurnId(id);
        request.setContent("현재 오류율이 높아서 롤백하겠습니다.");
        request.setSource("text");
        return request;
    }
}

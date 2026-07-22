package com.kapor.techtalk.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import com.kapor.techtalk.repository.TechTalkScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleplayService {
    private final TechTalkScenarioRepository scenarioRepository;
    private final RoleplaySessionRepository sessionRepository;

    public List<TechTalkScenario> getScenarios() {
        List<TechTalkScenario> scenarios = scenarioRepository.findByActiveTrue();
        if (!scenarios.isEmpty()) return scenarios;
        return List.of(
                scenarioRepository.save(defaultScenario("서버 장애 보고", "Báo cáo sự cố server", "backend", "intermediate",
                        "Báo cáo DB timeout, nêu phạm vi ảnh hưởng và phương án rollback.", "Kim Min-su", "Tech Lead", "Naver", "👨🏻‍💻",
                        List.of("장애", "서버", "오류", "모니터링"))),
                scenarioRepository.save(defaultScenario("요구사항 리뷰", "Review yêu cầu tính năng", "frontend", "beginner",
                        "Làm rõ yêu cầu UI animation với Product Owner.", "Lee Ji-eun", "Product Owner", "Toss", "👩🏻‍💼",
                        List.of("요구사항", "화면", "일정", "확인")))
        );
    }

    public TechTalkScenario getScenario(String id) {
        return scenarioRepository.findById(id)
                .filter(TechTalkScenario::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", "id", id));
    }

    private TechTalkScenario defaultScenario(String title, String titleVi, String domain, String difficulty,
                                              String missionVi, String name, String role, String company, String avatar,
                                              List<String> vocabulary) {
        return TechTalkScenario.builder().title(title).titleVi(titleVi).domain(domain).difficulty(difficulty)
                .missionVi(missionVi).persona(TechTalkScenario.Persona.builder().name(name).role(role)
                        .company(company).avatar(avatar).build()).requiredVocabulary(vocabulary).active(true)
                .objectives(List.of("Dùng kính ngữ phù hợp", "Nêu rõ tình huống và hành động tiếp theo"))
                .createdAt(Instant.now()).build();
    }

    public RoleplaySession start(String userId, String scenarioId) {
        TechTalkScenario scenario = getScenario(scenarioId);
        Instant now = Instant.now();
        RoleplaySession session = RoleplaySession.builder()
                .userId(userId).scenarioId(scenarioId).status("active").startedAt(now)
                .messages(new ArrayList<>(List.of(RoleplaySession.Message.builder()
                        .id(UUID.randomUUID().toString()).role("ai")
                        .content(openingMessage(scenario)).timestamp(now).build())))
                .build();
        return sessionRepository.save(session);
    }

    public RoleplaySession send(String userId, String sessionId, String content) {
        RoleplaySession session = ownedSession(userId, sessionId);
        if (!"active".equals(session.getStatus())) throw new IllegalArgumentException("Roleplay session is no longer active");
        Instant now = Instant.now();
        session.getMessages().add(RoleplaySession.Message.builder().id(UUID.randomUUID().toString())
                .role("user").content(content.trim()).timestamp(now).evaluation(evaluate(content)).build());
        session.getMessages().add(RoleplaySession.Message.builder().id(UUID.randomUUID().toString())
                .role("ai").content(nextMessage(content)).timestamp(Instant.now()).build());
        return sessionRepository.save(session);
    }

    public RoleplayHintDto hint(String userId, String sessionId) {
        RoleplaySession session = ownedSession(userId, sessionId);
        TechTalkScenario scenario = getScenario(session.getScenarioId());
        return RoleplayHintDto.builder()
                .keywords(scenario.getRequiredVocabulary())
                .sentenceStructure("[대상]에 문제가 발생했습니다. [영향]을 확인하고 있습니다.")
                .politenessTip("Tech Lead에게는 -습니다 또는 -주시기 바랍니다를 사용하세요.")
                .build();
    }

    public RoleplaySession end(String userId, String sessionId) {
        RoleplaySession session = ownedSession(userId, sessionId);
        if (!"completed".equals(session.getStatus())) {
            List<RoleplaySession.Evaluation> evaluations = session.getMessages().stream()
                    .filter(message -> message.getEvaluation() != null).map(RoleplaySession.Message::getEvaluation).toList();
            int grammar = average(evaluations, "grammar");
            int vocabulary = average(evaluations, "vocabulary");
            int politeness = average(evaluations, "politeness");
            int completion = Math.min(100, 35 + evaluations.size() * 20);
            session.setFinalEvaluation(RoleplaySession.FinalEvaluation.builder()
                    .grammar(grammar).vocabulary(vocabulary).politeness(politeness).taskCompletion(completion)
                    .overallScore(Math.round((grammar + vocabulary + politeness + completion) / 4f))
                    .feedback("전반적으로 잘하셨습니다. 상황과 영향 범위를 더 구체적으로 설명해 보세요.")
                    .feedbackVi("Bạn đã làm tốt. Hãy mô tả cụ thể hơn về tình huống và phạm vi ảnh hưởng.")
                    .improvementAreas(List.of("Nêu thời điểm xảy ra sự cố", "Dùng kính ngữ nhất quán"))
                    .build());
            session.setStatus("completed");
            session.setEndedAt(Instant.now());
            session = sessionRepository.save(session);
        }
        return session;
    }

    public List<RoleplaySession> history(String userId) { return sessionRepository.findByUserIdOrderByStartedAtDesc(userId); }

    private RoleplaySession ownedSession(String userId, String sessionId) {
        return sessionRepository.findById(sessionId).filter(session -> userId.equals(session.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Roleplay session", "id", sessionId));
    }

    private RoleplaySession.Evaluation evaluate(String content) {
        int politeness = (content.contains("습니다") || content.contains("드립니다")) ? 92 : 70;
        int vocabulary = (content.contains("서버") || content.contains("장애") || content.contains("배포")) ? 88 : 72;
        return RoleplaySession.Evaluation.builder().grammar(content.endsWith(".") ? 88 : 78)
                .vocabulary(vocabulary).politeness(politeness)
                .corrections(politeness < 80 ? List.of("Tech Lead에게는 -습니다를 사용해 보세요.") : List.of()).build();
    }

    private String openingMessage(TechTalkScenario scenario) { return "안녕하세요. " + scenario.getMissionVi() + "에 대해 보고해 주시겠어요?"; }
    private String nextMessage(String content) { return content.contains("장애") ? "알겠습니다. 장애 발생 시간과 영향 범위를 구체적으로 알려 주세요." : "좋습니다. 다음으로 원인과 대응 방안을 설명해 주세요."; }
    private int average(List<RoleplaySession.Evaluation> values, String metric) {
        if (values.isEmpty()) return 0;
        return Math.round((float) values.stream().mapToInt(value -> switch (metric) {
            case "grammar" -> value.getGrammar(); case "vocabulary" -> value.getVocabulary(); default -> value.getPoliteness();
        }).average().orElse(0));
    }
}

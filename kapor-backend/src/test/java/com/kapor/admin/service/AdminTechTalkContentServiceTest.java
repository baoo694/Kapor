package com.kapor.admin.service;

import com.kapor.admin.model.AdminPrompt;
import com.kapor.admin.repository.AdminPromptRepository;
import com.kapor.pronunciation.repository.PronunciationExerciseRepository;
import com.kapor.techtalk.dto.AdminScenarioRequest;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import com.kapor.techtalk.repository.TechTalkScenarioRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTechTalkContentServiceTest {
    private final TechTalkScenarioRepository scenarioRepository = mock(TechTalkScenarioRepository.class);
    private final RoleplaySessionRepository sessionRepository = mock(RoleplaySessionRepository.class);
    private final AdminPromptRepository promptRepository = mock(AdminPromptRepository.class);
    private final AdminContentService service = new AdminContentService(
            scenarioRepository,
            sessionRepository,
            mock(PronunciationExerciseRepository.class),
            promptRepository);

    @Test
    void createsAValidatedVersionedScenario() {
        AdminPrompt published = AdminPrompt.builder().id("prompt-1").status("published").build();
        when(promptRepository.findById("prompt-1")).thenReturn(Optional.of(published));
        when(scenarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminScenarioRequest request = validScenario();

        TechTalkScenario saved = service.createScenario(request);

        assertThat(saved.getSchemaVersion()).isEqualTo(2);
        assertThat(saved.getPromptTemplateId()).isEqualTo("prompt-1");
        assertThat(saved.getMission().getRequiredVocabulary()).containsExactly("장애", "롤백");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsEvaluationWeightsThatDoNotTotalOne() {
        AdminScenarioRequest request = validScenario();
        request.getEvaluationCriteria().setGrammarWeight(.8);
        request.setPromptTemplateId(null);

        assertThatThrownBy(() -> service.createScenario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("add up to 1.0");
        verify(scenarioRepository, never()).save(any());
    }

    @Test
    void archivesAScenarioWhenLearnerSessionsReferenceIt() {
        TechTalkScenario existing = TechTalkScenario.builder()
                .id("scenario-1").title("장애 보고").active(true).build();
        when(scenarioRepository.findById("scenario-1")).thenReturn(Optional.of(existing));
        when(sessionRepository.existsByScenarioId("scenario-1")).thenReturn(true);

        service.deleteScenario("scenario-1");

        assertThat(existing.isActive()).isFalse();
        verify(scenarioRepository).save(existing);
        verify(scenarioRepository, never()).delete(existing);
    }

    @Test
    void publishingDraftArchivesThePreviousRuntimeVersion() {
        AdminPrompt current = AdminPrompt.builder()
                .id("prompt-1").key("techtalk.roleplay.system")
                .promptVersion(1).status("published").content("old").build();
        AdminPrompt draft = AdminPrompt.builder()
                .id("prompt-2").key("techtalk.roleplay.system")
                .promptVersion(2).status("draft").content("new").requiredPlaceholders(List.of()).build();
        when(promptRepository.findById("prompt-2")).thenReturn(Optional.of(draft));
        when(promptRepository.findFirstByKeyAndStatusOrderByPromptVersionDesc(
                "techtalk.roleplay.system", "published")).thenReturn(Optional.of(current));
        when(promptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminPrompt published = service.publishPrompt("prompt-2");

        assertThat(current.getStatus()).isEqualTo("archived");
        assertThat(published.getStatus()).isEqualTo("published");
    }

    private AdminScenarioRequest validScenario() {
        AdminScenarioRequest request = new AdminScenarioRequest();
        request.setTitle("서버 장애 보고");
        request.setTitleVi("Báo cáo sự cố máy chủ");
        request.setDomain("backend");
        request.setDifficulty("intermediate");
        request.setOrder(10);
        request.setActive(true);
        request.setPromptTemplateId("prompt-1");
        request.setPersona(TechTalkScenario.Persona.builder()
                .name("김민수").role("Tech Lead").company("Kapor").build());
        request.setMission(TechTalkScenario.Mission.builder()
                .titleKo("장애 보고")
                .requiredVocabulary(List.of("장애", "롤백"))
                .objectives(List.of(TechTalkScenario.Objective.builder()
                        .ko("영향을 설명합니다.").vi("Giải thích ảnh hưởng.").build()))
                .build());
        request.setObjectives(List.of("Giải thích ảnh hưởng"));
        request.setRequiredVocabulary(List.of("장애", "롤백"));
        return request;
    }
}

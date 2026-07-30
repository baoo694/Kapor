package com.kapor.techtalk.service;

import com.kapor.admin.model.AdminPrompt;
import com.kapor.admin.repository.AdminPromptRepository;
import com.kapor.techtalk.model.TechTalkScenario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleplayPromptServiceTest {
    private final AdminPromptRepository repository = mock(AdminPromptRepository.class);
    private final RoleplayPromptService service = new RoleplayPromptService(repository);

    @Test
    void scenarioOverrideTakesPrecedenceAndRendersRuntimeValues() {
        TechTalkScenario scenario = scenario();
        scenario.setPromptOverride("{{personaName}} / {{missionTitle}} / {{requiredVocabulary}}");

        RoleplayPromptService.PromptSnapshot snapshot = service.resolve(scenario);

        assertThat(snapshot.content()).isEqualTo("김민수 / 장애 보고 / 장애, 롤백");
        assertThat(snapshot.version()).startsWith("scenario:");
        verifyNoInteractions(repository);
    }

    @Test
    void selectsPublishedPromptAndFreezesItsVersion() {
        AdminPrompt prompt = AdminPrompt.builder()
                .id("prompt-2")
                .key(RoleplayPromptService.ROLEPLAY_PROMPT_KEY)
                .promptVersion(2)
                .status("published")
                .content("Act as {{personaRole}} at {{company}}. {{objectives}}")
                .build();
        when(repository.findFirstByKeyAndStatusOrderByPromptVersionDesc(
                RoleplayPromptService.ROLEPLAY_PROMPT_KEY, "published"))
                .thenReturn(Optional.of(prompt));

        RoleplayPromptService.PromptSnapshot snapshot = service.resolve(scenario());

        assertThat(snapshot.content()).isEqualTo("Act as Tech Lead at Kapor. 장애를 설명합니다.");
        assertThat(snapshot.version()).isEqualTo("techtalk.roleplay.system:v2");
    }

    private TechTalkScenario scenario() {
        return TechTalkScenario.builder()
                .id("scenario-1")
                .title("서버 장애")
                .titleVi("Sự cố máy chủ")
                .difficulty("intermediate")
                .active(true)
                .persona(TechTalkScenario.Persona.builder()
                        .name("김민수")
                        .role("Tech Lead")
                        .company("Kapor")
                        .speechStyle("hasipsio")
                        .build())
                .mission(TechTalkScenario.Mission.builder()
                        .titleKo("장애 보고")
                        .objectives(List.of(TechTalkScenario.Objective.builder().ko("장애를 설명합니다.").build()))
                        .requiredVocabulary(List.of("장애", "롤백"))
                        .build())
                .build();
    }
}

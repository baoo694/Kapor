package com.kapor.honorifics.service;

import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HonorificsServiceTest {

    private final HonorificsAiService aiService = mock(HonorificsAiService.class);
    private final HonorificsService service = new HonorificsService(aiService);

    @Test
    void transformsCommonCasualBusinessKorean() {
        HonorificsAnalysisDto result = service.analyze("나 오늘 서버 배포 했어. 너 확인해봐.", "hasipsio");

        assertThat(result.getCurrentLevel()).isEqualTo("banmal");
        assertThat(result.getAnalysisSource()).isEqualTo("rule_based");
        assertThat(result.getCorrections()).isNotEmpty();
        assertThat(result.getTransformedText())
                .isEqualTo("저 오늘 서버 배포 했습니다. 담당자 확인해 보십시오.");
        verifyNoInteractions(aiService);
    }

    @Test
    void coversSubjectAndObjectHonorificsWithParticles() {
        HonorificsAnalysisDto result = service.analyze(
                "팀장님이 회의실에 있어요. 부장님이 승인했어요. 자료를 팀장님에게 줬어요.", "hasipsio");

        assertThat(result.getTransformedText())
                .isEqualTo("팀장님께서 회의실에 계십니다. 부장님께서 승인하셨습니다. 자료를 팀장님께 드렸습니다.");
        assertThat(result.getCorrections()).extracting("type")
                .contains("particle", "honorific");
        assertThat(result.getAnalysisSource()).isEqualTo("rule_based");
        verifyNoInteractions(aiService);
    }

    @Test
    void coversCommonBusinessEndingsAndRequestsWithoutAi() {
        HonorificsAnalysisDto result = service.analyze(
                "안녕하세요. 검토해 주세요. 배포했어요. 저는 확인할게요.", "hasipsio");

        assertThat(result.getCurrentLevel()).isEqualTo("heyohaet");
        assertThat(result.getTransformedText())
                .isEqualTo("안녕하십니까. 검토해 주십시오. 배포했습니다. 저는 확인하겠습니다.");
        assertThat(result.getAnalysisSource()).isEqualTo("rule_based");
        verifyNoInteractions(aiService);
    }

    @Test
    void recognizesFormalTextWithoutCallingAi() {
        HonorificsAnalysisDto result = service.analyze("서버 배포가 완료되었습니다.", "hasipsio");

        assertThat(result.getCurrentLevel()).isEqualTo("hasipsio");
        assertThat(result.getCorrections()).isEmpty();
        assertThat(result.getAnalysisSource()).isEqualTo("rule_based");
        verifyNoInteractions(aiService);
    }

    @Test
    void doesNotTurnAnHonorificPersonObjectIntoKkeseo() {
        HonorificsAnalysisDto result = service.analyze("제가 팀장님이 좋아요.", "hasipsio");

        assertThat(result.getTransformedText()).isEqualTo("제가 팀장님이 좋습니다.");
        assertThat(result.getTransformedText()).doesNotContain("께서");
        verifyNoInteractions(aiService);
    }

    @Test
    void usesAiFallbackForAnUnhandledCompleteCasualUtterance() {
        HonorificsAnalysisDto aiResult = HonorificsAnalysisDto.builder()
                .currentLevel("banmal")
                .confidence(0.91)
                .analysisSource("ai_fallback")
                .corrections(List.of())
                .transformedText("문서가 종료되었습니까?")
                .build();
        when(aiService.analyze("문서 멈췄어?", "hasipsio")).thenReturn(Optional.of(aiResult));

        HonorificsAnalysisDto result = service.analyze("문서 멈췄어?", "hasipsio");

        assertThat(result.getAnalysisSource()).isEqualTo("ai_fallback");
        assertThat(result.getTransformedText()).isEqualTo("문서가 종료되었습니까?");
    }
}

package com.kapor.honorifics.service;

import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HonorificsServiceTest {

    private final HonorificsService service = new HonorificsService();

    @Test
    void transformsCommonCasualBusinessKorean() {
        HonorificsAnalysisDto result = service.analyze("나 오늘 서버 배포 했어. 너 확인해봐.", "hasipsio");

        assertThat(result.getCurrentLevel()).isEqualTo("banmal");
        assertThat(result.getCorrections()).isNotEmpty();
        assertThat(result.getTransformedText()).contains("저").contains("하였습니다");
    }

    @Test
    void recognizesFormalTextWithoutInventingCorrections() {
        HonorificsAnalysisDto result = service.analyze("서버 배포가 완료되었습니다.", "hasipsio");

        assertThat(result.getCurrentLevel()).isEqualTo("hasipsio");
        assertThat(result.getCorrections()).isEmpty();
    }
}

package com.kapor.pronunciation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanReadingMatchScorerTest {
    private final KoreanReadingMatchScorer scorer = new KoreanReadingMatchScorer();

    @Test
    void givesFullReadingMatchForAnIdenticalKoreanTranscript() {
        var result = scorer.score("서버 배포가 완료되었습니다", "서버 배포가 완료되었습니다", 3.2d);

        assertThat(result.scores().getOverall()).isEqualTo(100);
        assertThat(result.scores().getAccuracy()).isEqualTo(100);
        assertThat(result.scores().getCompleteness()).isEqualTo(100);
        assertThat(result.wordFeedback()).extracting(feedback -> feedback.getScore()).containsOnly(100);
    }

    @Test
    void flagsAnOmittedTargetWordWithoutClaimingPhonemeFeedback() {
        var result = scorer.score("서버 배포가 완료되었습니다", "서버 완료되었습니다", 3.2d);

        assertThat(result.scores().getOverall()).isLessThan(100);
        assertThat(result.scores().getCompleteness()).isLessThan(100);
        assertThat(result.wordFeedback()).anySatisfy(feedback -> {
            assertThat(feedback.getText()).isEqualTo("배포가");
            assertThat(feedback.getScore()).isEqualTo(0);
            assertThat(feedback.getPhonemeDetail()).contains("Không nhận dạng");
        });
    }

    @Test
    void rejectsACompletelyDifferentSentence() {
        assertThat(scorer.isDifferentSentence(
                "비동기 처리를 구현했습니다", "네 안녕하세요 저는 파로고입니다", 0)).isTrue();
    }

    @Test
    void trustsAnIdenticalTranscriptEvenWhenAzureCompletenessIsLow() {
        assertThat(scorer.isDifferentSentence(
                "비동기 처리를 구현했습니다", "비동기 처리를 구현했습니다", 33)).isFalse();
    }

    @Test
    void keepsACloselyRelatedReadingForPronunciationFeedback() {
        assertThat(scorer.isDifferentSentence(
                "서버 배포가 완료되었습니다", "서버 완료되었습니다", 33)).isFalse();
    }

    @Test
    void asksForAnotherRecordingWhenWhisperXHeardNoWords() {
        assertThat(scorer.isDifferentSentence("비동기 처리를 구현했습니다", "", 0)).isTrue();
    }
}

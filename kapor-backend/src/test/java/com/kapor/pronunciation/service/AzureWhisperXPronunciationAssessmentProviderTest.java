package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.pronunciation.model.PronunciationAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AzureWhisperXPronunciationAssessmentProviderTest {

    @Test
    void rejectsAClearlyDifferentWhisperTranscriptBeforeCallingAzure() {
        AtomicBoolean azureCalled = new AtomicBoolean();
        AtomicBoolean geminiCalled = new AtomicBoolean();
        AzurePronunciationAssessor azure = new AzurePronunciationAssessor(null, null) {
            @Override
            public Result assess(String referenceText, byte[] wavAudio) {
                azureCalled.set(true);
                throw new AssertionError("Azure must not run after a failed WhisperX preflight.");
            }
        };
        WhisperXTranscriber whisperX = new WhisperXTranscriber(null) {
            @Override
            public PronunciationAttempt.Transcript transcribe(byte[] wavAudio, String referenceText) {
                return PronunciationAttempt.Transcript.builder().provider("whisperx")
                        .text("네 안녕하세요 저는 파로고입니다").durationSeconds(2.4d).build();
            }
        };
        GeminiPronunciationAnalyzer gemini = new GeminiPronunciationAnalyzer(WebClient.builder(), new ObjectMapper()) {
            @Override
            public PronunciationAttempt.Analysis explain(AzurePronunciationAssessor.Result azure,
                                                          PronunciationAttempt.Transcript transcript) {
                geminiCalled.set(true);
                throw new AssertionError("Gemini must not run after a failed WhisperX preflight.");
            }
        };
        AzureWhisperXPronunciationAssessmentProvider provider =
                new AzureWhisperXPronunciationAssessmentProvider(azure, whisperX, gemini, new KoreanReadingMatchScorer());
        byte[] audio = new byte[] {1, 2, 3};

        assertThatThrownBy(() -> provider.assess("user-1", "비동기 처리를 구현했습니다", audio))
                .isInstanceOf(WhisperPreflightRejectedException.class);

        assertThat(azureCalled).isFalse();
        assertThat(geminiCalled).isFalse();
    }
}

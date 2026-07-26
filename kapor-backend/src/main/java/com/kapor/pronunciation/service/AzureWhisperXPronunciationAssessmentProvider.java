package com.kapor.pronunciation.service;

import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * Combines independent evidence without allowing one provider to impersonate
 * another: Azure supplies scores, WhisperX supplies transcript/timeline, and
 * Gemini only turns the resulting facts into Vietnamese coaching text.
 */
@Service
@RequiredArgsConstructor
public class AzureWhisperXPronunciationAssessmentProvider implements PronunciationAssessmentProvider {
    private final AzurePronunciationAssessor azureAssessor;
    private final WhisperXTranscriber whisperXTranscriber;
    private final GeminiPronunciationAnalyzer geminiAnalyzer;

    @Override
    public String name() {
        return "azure_pa_whisperx_gemini";
    }

    @Override
    public Assessment assess(String userId, String referenceText, byte[] wavAudio) {
        AzurePronunciationAssessor.Result azure;
        PronunciationAttempt.Transcript transcript;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<AzurePronunciationAssessor.Result> azureFuture = executor.submit(() -> azureAssessor.assess(referenceText, wavAudio));
            Future<PronunciationAttempt.Transcript> transcriptFuture = executor.submit(
                    () -> whisperXTranscriber.transcribe(wavAudio, referenceText));
            azure = result(azureFuture);
            transcript = result(transcriptFuture);
        }

        PronunciationAttempt.Analysis analysis;
        try {
            analysis = geminiAnalyzer.explain(azure, transcript);
        } catch (RuntimeException ignored) {
            // A non-scoring explanatory model must never discard an Azure PA result.
            analysis = deterministicFallback(azure.words());
        }
        return new Assessment(azure.scores(), transcript.getText(), azure.words(), analysis, transcript,
                "azure-pa", "whisperx");
    }

    private <T> T result(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Đánh giá phát âm đã bị gián đoạn.", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Không thể hoàn tất đánh giá phát âm.", exception.getCause());
        }
    }

    private PronunciationAttempt.Analysis deterministicFallback(List<PronunciationAttempt.WordFeedback> words) {
        List<PronunciationAttempt.Interpretation> items = IntStream.range(0, words.size())
                .filter(index -> needsPractice(words.get(index))).limit(3)
                .mapToObj(index -> PronunciationAttempt.Interpretation.builder()
                        .wordIndex(index)
                        .explanationVi("Azure đánh dấu từ này cần luyện thêm.")
                        .practiceTipVi("Nghe câu mẫu, đọc chậm từ này rồi ghép lại vào cả câu.").build()).toList();
        String summary = items.isEmpty()
                ? "Các từ trong câu mẫu đều đạt mức ổn định theo Azure. Hãy tiếp tục giữ nhịp đọc tự nhiên."
                : "Dựa trên kết quả Azure, hãy luyện chậm các từ được đánh dấu rồi đọc lại cả câu.";
        return PronunciationAttempt.Analysis.builder().provider("deterministic-fallback").status("unavailable")
                .summaryVi(summary).correctedText("").grammarNoteVi("").interpretations(items).build();
    }

    private boolean needsPractice(PronunciationAttempt.WordFeedback word) {
        return (word.getErrorType() != null && !"none".equalsIgnoreCase(word.getErrorType()))
                || (word.getScore() != null && word.getScore() < 85);
    }
}

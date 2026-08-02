package com.kapor.pronunciation.service;

import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * WhisperX first verifies that the learner read the exercise sentence. Only
 * then does Azure score pronunciation; Gemini only turns those facts into
 * Vietnamese coaching text.
 */
@Service
@RequiredArgsConstructor
public class AzureWhisperXPronunciationAssessmentProvider implements PronunciationAssessmentProvider {
    private final AzurePronunciationAssessor azureAssessor;
    private final WhisperXTranscriber whisperXTranscriber;
    private final GeminiPronunciationAnalyzer geminiAnalyzer;
    private final KoreanReadingMatchScorer readingMatchScorer;

    @Override
    public String name() {
        return "azure_pa_whisperx_gemini";
    }

    @Override
    public Assessment assess(String userId, String referenceText, byte[] wavAudio) {
        PronunciationAttempt.Transcript transcript = whisperXTranscriber.transcribe(wavAudio, referenceText);
        if (readingMatchScorer.isClearlyDifferentSentence(referenceText, transcript.getText())) {
            throw new WhisperPreflightRejectedException(transcript);
        }
        AzurePronunciationAssessor.Result azure = azureAssessor.assess(referenceText, wavAudio);
        Integer azureCompleteness = azure.scores() == null ? null : azure.scores().getCompleteness();
        // Azure's Korean CompletenessScore can penalize an imprecise but still
        // present word. The UI's “Đủ từ” therefore represents WhisperX word
        // coverage, while raw Azure completeness remains internal evidence for
        // borderline wrong-sentence detection.
        if (azure.scores() != null) {
            azure.scores().setCompleteness(readingMatchScorer.completeness(referenceText, transcript.getText()));
        }

        PronunciationAttempt.Analysis analysis;
        try {
            analysis = geminiAnalyzer.explain(azure, transcript);
        } catch (RuntimeException ignored) {
            // A non-scoring explanatory model must never discard an Azure PA result.
            analysis = deterministicFallback(azure.words());
        }
        return new Assessment(azure.scores(), transcript.getText(), azure.words(), analysis, transcript,
                "azure-pa", "whisperx", azureCompleteness);
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

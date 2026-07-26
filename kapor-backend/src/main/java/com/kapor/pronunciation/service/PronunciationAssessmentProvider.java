package com.kapor.pronunciation.service;

import com.kapor.pronunciation.model.PronunciationAttempt;

import java.util.List;

/**
 * Isolates the application from a particular speech-assessment vendor. The
 * application owns its API contract; provider-specific payloads never leave
 * this boundary.
 */
public interface PronunciationAssessmentProvider {

    default String name() { return "unknown"; }

    Assessment assess(String userId, String referenceText, byte[] wavAudio);

    record Assessment(
            PronunciationAttempt.Scores scores,
            String transcription,
            List<PronunciationAttempt.WordFeedback> wordFeedback,
            PronunciationAttempt.Analysis analysis,
            PronunciationAttempt.Transcript transcript,
            String assessmentProvider,
            String transcriptProvider) {

        /**
         * Compatibility constructor for the previous, single-provider test
         * fixtures. New production implementations must provide provenance
         * for both the assessment and transcript.
         */
        public Assessment(PronunciationAttempt.Scores scores,
                          String transcription,
                          List<PronunciationAttempt.WordFeedback> wordFeedback,
                          PronunciationAttempt.Analysis analysis) {
            this(scores, transcription, wordFeedback, analysis, null, null, null);
        }
    }
}

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
            PronunciationAttempt.Analysis analysis) {
    }
}

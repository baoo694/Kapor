package com.kapor.pronunciation.service;

import com.kapor.pronunciation.model.PronunciationAttempt;

/**
 * Internal control flow for a recording that WhisperX shows is clearly not
 * the exercise sentence. This is a normal learner outcome, not a provider
 * failure, so the service can persist it as {@code wrong_sentence}.
 */
final class WhisperPreflightRejectedException extends RuntimeException {
    private final PronunciationAttempt.Transcript transcript;

    WhisperPreflightRejectedException(PronunciationAttempt.Transcript transcript) {
        super("WhisperX transcript is clearly different from the reference sentence.");
        this.transcript = transcript;
    }

    PronunciationAttempt.Transcript transcript() {
        return transcript;
    }
}

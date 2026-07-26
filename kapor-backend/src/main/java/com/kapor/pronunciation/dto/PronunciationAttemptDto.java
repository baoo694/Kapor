package com.kapor.pronunciation.dto;

import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** Safe history representation: never exposes user IDs or private object-storage keys. */
@Data
@Builder
public class PronunciationAttemptDto {
    private String id;
    private String exerciseId;
    private int sentenceIndex;
    private String status;
    private String assessmentVersion;
    private String assessmentProvider;
    private String transcriptProvider;
    private PronunciationAttempt.Scores scores;
    private String transcriptionText;
    private PronunciationAttempt.Transcript transcript;
    private PronunciationAttempt.Analysis analysis;
    private List<PronunciationAttempt.WordFeedback> assessmentWords;
    /** @deprecated Compatibility alias for assessmentWords. */
    private List<PronunciationAttempt.WordFeedback> transcription;
    private List<Double> userWaveform;
    private Instant attemptedAt;
    private Long audioDurationMs;
    private String attemptAudioUrl;
}

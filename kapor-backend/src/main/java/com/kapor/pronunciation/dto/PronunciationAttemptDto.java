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
    private PronunciationAttempt.Scores scores;
    private String transcriptionText;
    private PronunciationAttempt.Analysis analysis;
    private List<PronunciationAttempt.WordFeedback> transcription;
    private List<Double> userWaveform;
    private Instant attemptedAt;
    private Long audioDurationMs;
    private String attemptAudioUrl;
}

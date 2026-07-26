package com.kapor.pronunciation.dto;

import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PronunciationEvaluationDto {
    private String attemptId;
    private String status;
    private String message;
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
    private List<Double> referenceWaveform;
    private List<Double> userWaveform;
    private String attemptAudioUrl;
}

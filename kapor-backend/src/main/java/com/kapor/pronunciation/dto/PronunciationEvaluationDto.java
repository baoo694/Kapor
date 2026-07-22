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
    private PronunciationAttempt.Scores scores;
    private List<PronunciationAttempt.WordFeedback> transcription;
    private List<Double> referenceWaveform;
    private List<Double> userWaveform;
}

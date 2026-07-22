package com.kapor.pronunciation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pronunciation_attempts")
public class PronunciationAttempt {
    @Id private String id;
    private String userId;
    private String exerciseId;
    private int sentenceIndex;
    private String status;
    private Scores scores;
    @Builder.Default private List<WordFeedback> transcription = new ArrayList<>();
    @Builder.Default private List<Double> userWaveform = new ArrayList<>();
    private Instant attemptedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Scores {
        private Integer accuracy;
        private Integer fluency;
        private Integer completeness;
        private Integer overall;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WordFeedback {
        private String text;
        private String accuracy;
        private String phonemeDetail;
    }
}

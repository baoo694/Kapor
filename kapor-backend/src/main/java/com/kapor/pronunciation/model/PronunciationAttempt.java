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
    private String provider;
    private String audioObjectKey;
    private String audioContentType;
    private Long audioDurationMs;
    private Scores scores;
    private String transcriptionText;
    private Analysis analysis;
    @Builder.Default private List<WordFeedback> transcription = new ArrayList<>();
    @Builder.Default private List<Double> userWaveform = new ArrayList<>();
    private Instant attemptedAt;
    private Instant expiresAt;

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
        private Integer score;
        private String accuracy;
        private String phonemeDetail;
    }

    /** Learner-facing analysis produced from the Whisper transcript by Gemini. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Analysis {
        private String summaryVi;
        private String correctedText;
        private String grammarNoteVi;
    }
}

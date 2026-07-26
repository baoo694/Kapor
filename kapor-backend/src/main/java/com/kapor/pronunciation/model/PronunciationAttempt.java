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
    /** Versioned to ensure legacy transcript-match attempts are never presented as Azure PA. */
    private String assessmentVersion;
    private String audioObjectKey;
    private String audioContentType;
    private Long audioDurationMs;
    private Scores scores;
    /** Azure PA word/phoneme evidence. Kept separate from the WhisperX transcript. */
    @Builder.Default private List<WordFeedback> assessmentWords = new ArrayList<>();
    private String transcriptionText;
    /** WhisperX transcript and word timestamps used exclusively by the audio timeline. */
    private Transcript transcript;
    private Analysis analysis;
    /** @deprecated Legacy field retained so clients can render attempts created before v2. */
    @Builder.Default private List<WordFeedback> transcription = new ArrayList<>();
    @Builder.Default private List<Double> userWaveform = new ArrayList<>();
    private Instant attemptedAt;
    private Instant expiresAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Scores {
        private Integer accuracy;
        private Integer fluency;
        private Integer completeness;
        /** Azure's PronScore. overall is retained for older mobile clients. */
        private Integer pronunciation;
        private Integer overall;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WordFeedback {
        private String text;
        private Integer score;
        private String accuracy;
        private String errorType;
        private Long offsetMs;
        private Long durationMs;
        private String phonemeDetail;
        @Builder.Default private List<PhonemeFeedback> phonemes = new ArrayList<>();
    }

    /** Azure returns scores for Korean phonemes but does not provide phoneme names. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PhonemeFeedback {
        private Integer index;
        private Integer score;
        private String phoneme;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Transcript {
        private String provider;
        private String text;
        private Double durationSeconds;
        @Builder.Default private List<TranscriptWord> words = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TranscriptWord {
        private String text;
        private Double startSeconds;
        private Double endSeconds;
        private Double confidence;
        /** aligned or unaligned. The UI must never invent a timestamp. */
        private String timingStatus;
    }

    /** Learner-facing Vietnamese interpretation. Gemini never produces scores. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Analysis {
        private String provider;
        private String status;
        private String summaryVi;
        /** Retained only for legacy records. New Gemini responses leave this empty. */
        private String correctedText;
        /** Retained only for legacy records. New Gemini responses leave this empty. */
        private String grammarNoteVi;
        @Builder.Default private List<Interpretation> interpretations = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Interpretation {
        private Integer wordIndex;
        private String explanationVi;
        private String practiceTipVi;
    }
}

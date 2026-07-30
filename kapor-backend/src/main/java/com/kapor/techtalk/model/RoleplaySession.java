package com.kapor.techtalk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roleplay_sessions")
public class RoleplaySession {
    @Id
    private String id;
    private String userId;
    private String scenarioId;
    private String status;
    private boolean testMode;
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    @Builder.Default
    private List<Turn> turns = new ArrayList<>();
    private int hintsUsed;
    private String promptSnapshot;
    private String promptVersion;
    private String modelName;
    private FinalEvaluation finalEvaluation;
    private Instant startedAt;
    private Instant lastActivityAt;
    private Instant endedAt;
    private Long durationSeconds;
    @Version
    private Long version;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Message {
        private String id;
        private String clientTurnId;
        private String role;
        private String content;
        private String source;
        private String transcript;
        private String audioId;
        private String generationStatus;
        private Instant timestamp;
        private Evaluation evaluation;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Evaluation {
        private int grammar;
        private int vocabulary;
        private int politeness;
        private String status;
        private String feedbackVi;
        @Builder.Default
        private List<Correction> corrections = new ArrayList<>();
        @Builder.Default
        private List<String> usedRequiredVocabulary = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Correction {
        private String original;
        private String suggestion;
        private String type;
        private String noteVi;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Turn {
        private String id;
        private String clientTurnId;
        private String userMessageId;
        private String aiMessageId;
        private String status;
        private String errorCode;
        private Instant createdAt;
        private Instant completedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FinalEvaluation {
        private int overallScore;
        private int grammar;
        private int vocabulary;
        private int politeness;
        private int taskCompletion;
        private String feedback;
        private String feedbackVi;
        @Builder.Default
        private List<String> improvementAreas = new ArrayList<>();
        @Builder.Default
        private List<ObjectiveResult> objectives = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ObjectiveResult {
        private String objective;
        private boolean completed;
        private String evidence;
    }
}

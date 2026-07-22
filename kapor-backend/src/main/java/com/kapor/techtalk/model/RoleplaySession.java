package com.kapor.techtalk.model;

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
@Document(collection = "roleplay_sessions")
public class RoleplaySession {
    @Id
    private String id;
    private String userId;
    private String scenarioId;
    private String status;
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    private FinalEvaluation finalEvaluation;
    private Instant startedAt;
    private Instant endedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Message {
        private String id;
        private String role;
        private String content;
        private Instant timestamp;
        private Evaluation evaluation;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Evaluation {
        private int grammar;
        private int vocabulary;
        private int politeness;
        @Builder.Default
        private List<String> corrections = new ArrayList<>();
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
    }
}

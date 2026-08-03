package com.kapor.techtalk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "techtalk_scenarios")
public class TechTalkScenario {
    @Id
    private String id;
    private String title;
    private String titleVi;
    private String domain;
    private String difficulty;
    @Builder.Default
    private List<String> goalTags = new ArrayList<>();
    private Integer order;
    private Persona persona;
    /** Legacy Vietnamese summary retained for existing mobile clients. */
    private String missionVi;
    private Mission mission;
    @Builder.Default
    private List<String> objectives = new ArrayList<>();
    @Builder.Default
    private List<String> requiredVocabulary = new ArrayList<>();
    private EvaluationCriteria evaluationCriteria;
    private String promptTemplateId;
    private String promptOverride;
    private boolean active;
    @Builder.Default
    private int schemaVersion = 2;
    @Version
    private Long version;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Persona {
        private String name;
        private String role;
        private String company;
        /** Legacy emoji/avatar value retained for existing records. */
        private String avatar;
        private String avatarUrl;
        private String speechStyle;
        private String personality;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Mission {
        private String titleKo;
        private String titleVi;
        @Builder.Default
        private List<Objective> objectives = new ArrayList<>();
        private String contextPrompt;
        @Builder.Default
        private List<String> requiredVocabulary = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Objective {
        private String ko;
        private String vi;
        private String en;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EvaluationCriteria {
        @Builder.Default private double grammarWeight = 0.30;
        @Builder.Default private double vocabularyWeight = 0.30;
        @Builder.Default private double politenessWeight = 0.25;
        @Builder.Default private double taskCompletionWeight = 0.15;
    }
}

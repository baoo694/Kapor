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
@Document(collection = "techtalk_scenarios")
public class TechTalkScenario {
    @Id
    private String id;
    private String title;
    private String titleVi;
    private String domain;
    private String difficulty;
    private Persona persona;
    private String missionVi;
    @Builder.Default
    private List<String> objectives = new ArrayList<>();
    @Builder.Default
    private List<String> requiredVocabulary = new ArrayList<>();
    private boolean active;
    private Instant createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Persona {
        private String name;
        private String role;
        private String company;
        private String avatar;
    }
}

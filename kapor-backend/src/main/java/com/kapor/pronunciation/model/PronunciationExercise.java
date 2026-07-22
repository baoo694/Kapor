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
@Document(collection = "pronunciation_exercises")
public class PronunciationExercise {
    @Id private String id;
    private String title;
    private String titleVi;
    private String domain;
    private String difficulty;
    @Builder.Default private List<Sentence> sentences = new ArrayList<>();
    private int order;
    private Instant createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Sentence {
        private String text;
        private String translationVi;
        private String audioUrl;
        @Builder.Default private List<Double> waveformData = new ArrayList<>();
    }
}

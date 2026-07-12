package com.kapor.devvocab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lessons")
public class Lesson {

    @Id
    private String id;
    
    private String topicId;
    
    private String title;
    private String titleVi;
    
    private String content; // Markdown or rich text
    private String contentVi;
    
    private int order;
    
    private List<VocabularyItem> vocabulary;
    private List<Exercise> exercises;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VocabularyItem {
        private String id;
        private String korean;
        private String pronunciation;
        private String vietnamese;
        private String english;
        private String context;
        private String codeSnippet;
        private String audioUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exercise {
        private String id;
        private String type; // "multiple_choice", "fill_blank"
        private String question;
        private String questionVi;
        private List<String> options;
        private String correctAnswer;
    }
}

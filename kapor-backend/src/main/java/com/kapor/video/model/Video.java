package com.kapor.video.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "videos")
public class Video {
    @Id
    private String id;
    private String title;
    private String titleVi;
    private String youtubeUrl;
    private String youtubeVideoId;
    private String thumbnailUrl;
    private String domain;
    private String difficulty;
    private Integer durationSeconds;
    @Builder.Default
    private List<SubtitleLine> koreanSubtitles = new ArrayList<>();
    @Builder.Default
    private List<SubtitleLine> vietnameseSubtitles = new ArrayList<>();
    @Builder.Default
    private List<QuizMarker> quizMarkers = new ArrayList<>();
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtitleLine {
        private double start;
        private double end;
        private String text;
        @Builder.Default
        private List<TokenizedWord> tokens = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenizedWord {
        private String surface;
        private String stem;
        private String pos;
        private String pronunciation;
        private String meaningVi;
        private String meaningEn;
        private String definitionEn;
        private String exampleKo;
        private String grammarNote;
        private boolean clickable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizMarker {
        private String id;
        private double timestamp;
        private String question;
        private String questionVi;
        @Builder.Default
        private List<String> options = new ArrayList<>();
        private Integer correctAnswer;
    }
}

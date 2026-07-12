package com.kapor.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_activities")
@CompoundIndex(name = "userId_1_date_-1", def = "{'userId': 1, 'date': -1}", unique = true)
public class DailyActivity {

    @Id
    private String id;

    private String userId;
    private LocalDate date;

    @Builder.Default
    private int cardsReviewed = 0;
    
    @Builder.Default
    private int minutesStudied = 0;
    
    @Builder.Default
    private int roleplaySessions = 0;
    
    @Builder.Default
    private int lessonsCompleted = 0;

    @Builder.Default
    private int videosWatched = 0;

    // e.g. "speaking", "vocabulary", "listening", "roleplay"
    @Builder.Default
    private DailyMetrics metrics = new DailyMetrics();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetrics {
        @Builder.Default
        private int speakingScore = 0; // 0-100
        @Builder.Default
        private int vocabularyScore = 0; // 0-100
        @Builder.Default
        private int listeningScore = 0; // 0-100
        @Builder.Default
        private int roleplayScore = 0; // 0-100
    }
}

package com.kapor.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    private StreakInfo streak;
    private ProgressMetrics progress;
    private RecommendationCard recommendation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakInfo {
        private int currentStreak;
        private int longestStreak;
        private boolean isActiveToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressMetrics {
        private String period; // "weekly" | "monthly"
        private int speaking; // 0-100
        private int vocabulary; // 0-100
        private int listening; // 0-100
        private int roleplayScore; // 0-100
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationCard {
        private String type; // "resume_lesson" | "next_challenge" | "review_due" | "new_topic"
        private String title;
        private String subtitle;
        private String targetScreen;
        private String icon;
    }
}

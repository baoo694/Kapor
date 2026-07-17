package com.kapor.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {
    private long users;
    private long contentCount;
    private long dau;

    private List<UserGrowthPoint> userGrowthData;
    private List<DauPoint> dauData;
    private List<DomainCompletionPoint> lessonCompletionData;
    private List<AiUsagePoint> aiUsageData;
    private List<NewRegPoint> newRegData;
    private List<RetentionPoint> retentionData;
    private List<AiDailyPoint> aiDailyData;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserGrowthPoint {
        private String month;
        private int users;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DauPoint {
        private String day;
        private int dau;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DomainCompletionPoint {
        private String domain;
        private int completions;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiUsagePoint {
        private String month;
        private int cost;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewRegPoint {
        private String day;
        private int count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RetentionPoint {
        private String week;
        private int d1;
        private int d7;
        private int d30;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiDailyPoint {
        private String day;
        private int gemini;
        private int tts;
        private int stt;
    }
}

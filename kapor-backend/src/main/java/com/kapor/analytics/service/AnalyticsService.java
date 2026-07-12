package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final RecommendationService recommendationService;

    public DashboardResponse getDashboardData(String userId, String period) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 1. Streak Info
        LocalDate today = LocalDate.now();
        boolean isActiveToday = user.getStreak().getLastActiveDate() != null &&
                                user.getStreak().getLastActiveDate().equals(today);
        
        DashboardResponse.StreakInfo streakInfo = DashboardResponse.StreakInfo.builder()
                .currentStreak(user.getStreak().getCurrent())
                .longestStreak(user.getStreak().getLongest())
                .isActiveToday(isActiveToday)
                .build();

        // 2. Progress Metrics
        // Here we would normally aggregate daily_activities based on the period (weekly vs monthly)
        // For MVP, returning some aggregated dummy/default data based on past week
        LocalDate startDate = period.equals("monthly") ? today.minusDays(30) : today.minusDays(7);
        List<DailyActivity> activities = dailyActivityRepository.findByUserIdAndDateBetweenOrderByDateAsc(user.getId(), startDate, today);
        
        int avgSpeaking = 0;
        int avgVocab = 0;
        int avgListening = 0;
        int avgRoleplay = 0;

        if (!activities.isEmpty()) {
            avgSpeaking = (int) activities.stream().mapToInt(a -> a.getMetrics().getSpeakingScore()).average().orElse(0);
            avgVocab = (int) activities.stream().mapToInt(a -> a.getMetrics().getVocabularyScore()).average().orElse(0);
            avgListening = (int) activities.stream().mapToInt(a -> a.getMetrics().getListeningScore()).average().orElse(0);
            avgRoleplay = (int) activities.stream().mapToInt(a -> a.getMetrics().getRoleplayScore()).average().orElse(0);
        }

        DashboardResponse.ProgressMetrics progress = DashboardResponse.ProgressMetrics.builder()
                .period(period)
                .speaking(avgSpeaking)
                .vocabulary(avgVocab)
                .listening(avgListening)
                .roleplayScore(avgRoleplay)
                .build();

        // 3. Smart Recommendation
        DashboardResponse.RecommendationCard recommendation = recommendationService.generateRecommendation(user);

        return DashboardResponse.builder()
                .streak(streakInfo)
                .progress(progress)
                .recommendation(recommendation)
                .build();
    }
}

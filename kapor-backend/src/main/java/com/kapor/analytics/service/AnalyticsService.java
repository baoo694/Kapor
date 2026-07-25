package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final RecommendationService recommendationService;

    public DashboardResponse getDashboardData(String userId, String period) {
        return getDashboardData(userId, period, null);
    }

    public DashboardResponse getDashboardData(String userId, String period, Integer timezoneOffsetMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String normalizedPeriod = normalizePeriod(period);
        LocalDate today = today(timezoneOffsetMinutes);

        // 1. Streak Info
        boolean isActiveToday = user.getStreak().getLastActiveDate() != null &&
                                user.getStreak().getLastActiveDate().equals(today);
        
        DashboardResponse.StreakInfo streakInfo = DashboardResponse.StreakInfo.builder()
                .currentStreak(user.getStreak().getCurrent())
                .longestStreak(user.getStreak().getLongest())
                .isActiveToday(isActiveToday)
                .build();

        // 2. Progress Metrics. A week/month is a rolling 7/30-day window,
        // inclusive of the learner's local current day.
        LocalDate startDate = normalizedPeriod.equals("monthly") ? today.minusDays(29) : today.minusDays(6);
        // Do date-window filtering in Java. Mongo's conversion of LocalDate can
        // differ between the application container and older persisted records,
        // causing a valid activity on the boundary day to be missed.
        List<DailyActivity> activities = dailyActivityRepository.findByUserIdOrderByDateAsc(user.getId()).stream()
                .filter(activity -> activity.getDate() != null)
                .filter(activity -> !activity.getDate().isBefore(startDate) && !activity.getDate().isAfter(today))
                .toList();

        MetricAverage speaking = average(activities, Metric.SPEAKING);
        MetricAverage vocabulary = average(activities, Metric.VOCABULARY);
        MetricAverage listening = average(activities, Metric.LISTENING);
        MetricAverage roleplay = average(activities, Metric.ROLEPLAY);

        DashboardResponse.ProgressMetrics progress = DashboardResponse.ProgressMetrics.builder()
                .period(normalizedPeriod)
                .hasData(speaking.hasData || vocabulary.hasData || listening.hasData || roleplay.hasData)
                .activityDays(activities.size())
                .speaking(speaking.value)
                .vocabulary(vocabulary.value)
                .listening(listening.value)
                .roleplayScore(roleplay.value)
                .build();

        // 3. Smart Recommendation
        DashboardResponse.RecommendationCard recommendation = recommendationService.generateRecommendation(user);

        return DashboardResponse.builder()
                .streak(streakInfo)
                .progress(progress)
                .recommendation(recommendation)
                .build();
    }

    private MetricAverage average(List<DailyActivity> activities, Metric metric) {
        int total = 0;
        int samples = 0;

        for (DailyActivity activity : activities) {
            DailyActivity.DailyMetrics values = activity.getMetrics();
            if (values == null) continue;
            int score = switch (metric) {
                case SPEAKING -> values.getSpeakingScore();
                case VOCABULARY -> values.getVocabularyScore();
                case LISTENING -> values.getListeningScore();
                case ROLEPLAY -> values.getRoleplayScore();
            };
            int scoreTotal = switch (metric) {
                case SPEAKING -> values.getSpeakingScoreTotal();
                case VOCABULARY -> values.getVocabularyScoreTotal();
                case LISTENING -> values.getListeningScoreTotal();
                case ROLEPLAY -> values.getRoleplayScoreTotal();
            };
            int scoreSamples = switch (metric) {
                case SPEAKING -> values.getSpeakingSamples();
                case VOCABULARY -> values.getVocabularySamples();
                case LISTENING -> values.getListeningSamples();
                case ROLEPLAY -> values.getRoleplaySamples();
            };

            if (scoreSamples > 0) {
                total += scoreTotal;
                samples += scoreSamples;
            } else if (score != 0) {
                // Compatibility with DailyActivity documents created before
                // score totals/sample counts existed.
                total += score;
                samples++;
            }
        }

        if (samples > 0) {
            return new MetricAverage(Math.round((float) total / samples), true);
        }
        return new MetricAverage(0, false);
    }

    private String normalizePeriod(String period) {
        if ("weekly".equalsIgnoreCase(period)) return "weekly";
        if ("monthly".equalsIgnoreCase(period)) return "monthly";
        throw new IllegalArgumentException("period must be weekly or monthly");
    }

    private LocalDate today(Integer timezoneOffsetMinutes) {
        if (timezoneOffsetMinutes == null || timezoneOffsetMinutes < -18 * 60 || timezoneOffsetMinutes > 18 * 60) {
            return LocalDate.now();
        }
        return Instant.now().atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetMinutes * 60)).toLocalDate();
    }

    private enum Metric {
        SPEAKING,
        VOCABULARY,
        LISTENING,
        ROLEPLAY
    }

    private record MetricAverage(int value, boolean hasData) {
    }
}

package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailyActivityRepository dailyActivityRepository;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void includesTodaysVocabularyActivityFromTheUsersDailyHistory() {
        User user = User.builder().id("user-1").build();
        user.setStreak(User.Streak.builder().build());
        DailyActivity activity = DailyActivity.builder()
                .userId(user.getId())
                .date(LocalDate.now())
                .metrics(DailyActivity.DailyMetrics.builder()
                        .vocabularyScore(88)
                        .vocabularyScoreTotal(2200)
                        .vocabularySamples(25)
                        .build())
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(dailyActivityRepository.findByUserIdOrderByDateAsc(user.getId())).thenReturn(List.of(activity));

        DashboardResponse response = analyticsService.getDashboardData(user.getId(), "weekly", 420);

        assertThat(response.getProgress().isHasData()).isTrue();
        assertThat(response.getProgress().getActivityDays()).isEqualTo(1);
        assertThat(response.getProgress().getVocabulary()).isEqualTo(88);
        verify(dailyActivityRepository).findByUserIdOrderByDateAsc(user.getId());
    }

    @Test
    void calculatesDailyGoalUsingTodayStudyMinutes() {
        User user = User.builder().id("user-1").build();
        user.setStreak(User.Streak.builder().build());
        user.setSettings(User.UserSettings.builder().dailyGoalMinutes(10).build());
        DailyActivity activity = DailyActivity.builder().userId(user.getId()).date(LocalDate.now())
                .minutesStudied(7).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(dailyActivityRepository.findByUserIdOrderByDateAsc(user.getId())).thenReturn(List.of(activity));
        when(dailyActivityRepository.findByUserIdAndDate(user.getId(), LocalDate.now())).thenReturn(Optional.of(activity));

        DashboardResponse response = analyticsService.getDashboardData(user.getId(), "weekly", 420);

        assertThat(response.getDailyGoal().getTargetMinutes()).isEqualTo(10);
        assertThat(response.getDailyGoal().getStudiedMinutes()).isEqualTo(7);
        assertThat(response.getDailyGoal().getPercentComplete()).isEqualTo(70);
        assertThat(response.getDailyGoal().isCompleted()).isFalse();
    }
}

package com.kapor.analytics.service;

import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.model.LearningActivityEvent;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.analytics.repository.LearningActivityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityTrackingServiceTest {

    @Mock
    private LearningActivityEventRepository eventRepository;

    @Mock
    private DailyActivityRepository dailyActivityRepository;

    @Mock
    private StreakService streakService;

    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private ActivityTrackingService activityTrackingService;

    @Test
    void recordsAnEventAndUpdatesDailyMetrics() {
        when(eventRepository.existsByUserIdAndEventKey("user-1", "membyte-review-1")).thenReturn(false);
        when(dailyActivityRepository.findByUserIdAndDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        activityTrackingService.track("user-1", ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("membyte-review-1")
                .type("membyte_review")
                .cardsReviewed(1)
                .vocabularyScore(80)
                .build(), 420);

        ArgumentCaptor<DailyActivity> activityCaptor = ArgumentCaptor.forClass(DailyActivity.class);
        verify(eventRepository).insert(any(LearningActivityEvent.class));
        verify(dailyActivityRepository).save(activityCaptor.capture());
        verify(userStatsService).addActivity(org.mockito.ArgumentMatchers.eq("user-1"), any(LearningActivityEvent.class));
        verify(streakService).updateStreakForUser(org.mockito.ArgumentMatchers.eq("user-1"), any(LocalDate.class));

        DailyActivity activity = activityCaptor.getValue();
        assertThat(activity.getCardsReviewed()).isEqualTo(1);
        assertThat(activity.getMetrics().getVocabularyScore()).isEqualTo(80);
        assertThat(activity.getMetrics().getVocabularyScoreTotal()).isEqualTo(80);
        assertThat(activity.getMetrics().getVocabularySamples()).isEqualTo(1);
    }

    @Test
    void ignoresAnAlreadyRecordedEvent() {
        when(eventRepository.existsByUserIdAndEventKey("user-1", "event-1")).thenReturn(true);

        activityTrackingService.track("user-1", ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("event-1")
                .type("video_quiz")
                .listeningScore(100)
                .build(), 420);

        verify(eventRepository, never()).insert(any(LearningActivityEvent.class));
        verify(dailyActivityRepository, never()).save(any());
        verify(userStatsService, never()).addActivity(anyString(), any(LearningActivityEvent.class));
        verify(streakService, never()).updateStreakForUser(anyString(), any(LocalDate.class));
    }
}

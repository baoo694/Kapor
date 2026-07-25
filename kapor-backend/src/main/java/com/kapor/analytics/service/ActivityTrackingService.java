package com.kapor.analytics.service;

import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.model.LearningActivityEvent;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.analytics.repository.LearningActivityEventRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ActivityTrackingService {

    private final LearningActivityEventRepository eventRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final StreakService streakService;

    /**
     * Saves a learning event once and updates the per-day dashboard aggregate.
     * The event key must identify the completed action, not a UI attempt.
     */
    public void track(String userId, ActivityUpdate update, Integer timezoneOffsetMinutes) {
        if (userId == null || userId.isBlank() || update == null || update.getEventKey() == null
                || update.getEventKey().isBlank()) {
            return;
        }

        if (eventRepository.existsByUserIdAndEventKey(userId, update.getEventKey())) {
            return;
        }

        Instant now = Instant.now();
        LocalDate localDate = localDate(now, timezoneOffsetMinutes);
        LearningActivityEvent event = LearningActivityEvent.builder()
                .userId(userId)
                .eventKey(update.getEventKey())
                .type(update.getType())
                .occurredAt(now)
                .localDate(localDate)
                .cardsReviewed(nonNegative(update.getCardsReviewed()))
                .minutesStudied(nonNegative(update.getMinutesStudied()))
                .roleplaySessions(nonNegative(update.getRoleplaySessions()))
                .lessonsCompleted(nonNegative(update.getLessonsCompleted()))
                .videosWatched(nonNegative(update.getVideosWatched()))
                .speakingScore(score(update.getSpeakingScore()))
                .vocabularyScore(score(update.getVocabularyScore()))
                .listeningScore(score(update.getListeningScore()))
                .roleplayScore(score(update.getRoleplayScore()))
                .build();

        try {
            eventRepository.insert(event);
        } catch (DuplicateKeyException ignored) {
            // Another concurrent request has already applied this event.
            return;
        }

        DailyActivity activity = dailyActivityRepository.findByUserIdAndDate(userId, localDate)
                .orElseGet(() -> DailyActivity.builder()
                        .userId(userId)
                        .date(localDate)
                        .metrics(new DailyActivity.DailyMetrics())
                        .build());
        if (activity.getMetrics() == null) {
            activity.setMetrics(new DailyActivity.DailyMetrics());
        }

        activity.setCardsReviewed(activity.getCardsReviewed() + event.getCardsReviewed());
        activity.setMinutesStudied(activity.getMinutesStudied() + event.getMinutesStudied());
        activity.setRoleplaySessions(activity.getRoleplaySessions() + event.getRoleplaySessions());
        activity.setLessonsCompleted(activity.getLessonsCompleted() + event.getLessonsCompleted());
        activity.setVideosWatched(activity.getVideosWatched() + event.getVideosWatched());
        addMetric(activity.getMetrics(), Metric.SPEAKING, event.getSpeakingScore());
        addMetric(activity.getMetrics(), Metric.VOCABULARY, event.getVocabularyScore());
        addMetric(activity.getMetrics(), Metric.LISTENING, event.getListeningScore());
        addMetric(activity.getMetrics(), Metric.ROLEPLAY, event.getRoleplayScore());
        dailyActivityRepository.save(activity);

        streakService.updateStreakForUser(userId, localDate);
    }

    private void addMetric(DailyActivity.DailyMetrics metrics, Metric metric, Integer value) {
        if (value == null) {
            return;
        }
        int normalized = score(value);
        switch (metric) {
            case SPEAKING -> {
                int[] aggregate = priorAggregate(metrics.getSpeakingScore(), metrics.getSpeakingScoreTotal(),
                        metrics.getSpeakingSamples());
                metrics.setSpeakingScoreTotal(aggregate[0] + normalized);
                metrics.setSpeakingSamples(aggregate[1] + 1);
                metrics.setSpeakingScore(Math.round((float) metrics.getSpeakingScoreTotal() / metrics.getSpeakingSamples()));
            }
            case VOCABULARY -> {
                int[] aggregate = priorAggregate(metrics.getVocabularyScore(), metrics.getVocabularyScoreTotal(),
                        metrics.getVocabularySamples());
                metrics.setVocabularyScoreTotal(aggregate[0] + normalized);
                metrics.setVocabularySamples(aggregate[1] + 1);
                metrics.setVocabularyScore(Math.round((float) metrics.getVocabularyScoreTotal() / metrics.getVocabularySamples()));
            }
            case LISTENING -> {
                int[] aggregate = priorAggregate(metrics.getListeningScore(), metrics.getListeningScoreTotal(),
                        metrics.getListeningSamples());
                metrics.setListeningScoreTotal(aggregate[0] + normalized);
                metrics.setListeningSamples(aggregate[1] + 1);
                metrics.setListeningScore(Math.round((float) metrics.getListeningScoreTotal() / metrics.getListeningSamples()));
            }
            case ROLEPLAY -> {
                int[] aggregate = priorAggregate(metrics.getRoleplayScore(), metrics.getRoleplayScoreTotal(),
                        metrics.getRoleplaySamples());
                metrics.setRoleplayScoreTotal(aggregate[0] + normalized);
                metrics.setRoleplaySamples(aggregate[1] + 1);
                metrics.setRoleplayScore(Math.round((float) metrics.getRoleplayScoreTotal() / metrics.getRoleplaySamples()));
            }
        }
    }

    private int[] priorAggregate(int legacyScore, int total, int samples) {
        if (samples > 0) {
            return new int[]{total, samples};
        }
        // Keep documents created before the event log meaningful when they are
        // extended by a new activity on the same date.
        return legacyScore == 0 ? new int[]{0, 0} : new int[]{legacyScore, 1};
    }

    private LocalDate localDate(Instant instant, Integer timezoneOffsetMinutes) {
        if (timezoneOffsetMinutes == null || timezoneOffsetMinutes < -18 * 60 || timezoneOffsetMinutes > 18 * 60) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return instant.atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetMinutes * 60)).toLocalDate();
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }

    private Integer score(Integer value) {
        return value == null ? null : Math.max(0, Math.min(100, value));
    }

    private enum Metric {
        SPEAKING,
        VOCABULARY,
        LISTENING,
        ROLEPLAY
    }

    @Value
    @Builder
    public static class ActivityUpdate {
        String eventKey;
        String type;
        @Builder.Default int cardsReviewed = 0;
        @Builder.Default int minutesStudied = 0;
        @Builder.Default int roleplaySessions = 0;
        @Builder.Default int lessonsCompleted = 0;
        @Builder.Default int videosWatched = 0;
        Integer speakingScore;
        Integer vocabularyScore;
        Integer listeningScore;
        Integer roleplayScore;
    }
}

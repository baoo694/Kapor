package com.kapor.membyte.service;

import com.kapor.membyte.dto.MembyteReviewRatingRequest.Rating;
import com.kapor.membyte.model.MembyteFlashcard;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Small, persisted FSRS scheduler. It uses FSRS stability, difficulty and
 * retrievability equations, with short learning steps for a card's first review.
 */
@Service
public class FsrsService {

    private static final double[] W = {
            0.4197, 1.0921, 5.9172, 0.0117, 1.6172, 1.2172, 0.0812, 0.1544,
            1.0824, 0.1833, 0.5015, 1.5674, 0.4525, 2.7473, 0.2223, 0.8267, 1.2503
    };
    private static final double DESIRED_RETENTION = 0.90;
    private static final double MIN_STABILITY = 0.1;
    private static final double MAX_INTERVAL_DAYS = 36500;

    public Schedule preview(MembyteFlashcard card, Rating rating, Instant now) {
        return calculate(card, rating, now);
    }

    public Schedule rate(MembyteFlashcard card, Rating rating, Instant now) {
        Schedule schedule = calculate(card, rating, now);
        card.setNew(false);
        card.setDueAt(schedule.getDueAt());
        card.setLastReviewedAt(now);
        card.setDifficulty(schedule.getDifficulty());
        card.setStability(schedule.getStability());
        card.setRepetitions(card.getRepetitions() + 1);
        if (rating == Rating.AGAIN) {
            card.setLapses(card.getLapses() + 1);
        }
        return schedule;
    }

    private Schedule calculate(MembyteFlashcard card, Rating rating, Instant now) {
        double stability;
        double difficulty;
        boolean isNew = card.isNew() || card.getStability() <= 0 || card.getDifficulty() <= 0;

        if (isNew) {
            stability = initialStability(rating);
            difficulty = initialDifficulty(rating);
            return new Schedule(firstReviewDueAt(rating, now), difficulty, stability);
        }

        double elapsedDays = elapsedDays(card.getLastReviewedAt(), now);
        double retrievability = Math.exp(Math.log(DESIRED_RETENTION) * elapsedDays / card.getStability());
        difficulty = nextDifficulty(card.getDifficulty(), rating);
        stability = rating == Rating.AGAIN
                ? nextForgetStability(card.getStability(), card.getDifficulty(), retrievability)
                : nextRecallStability(card.getStability(), card.getDifficulty(), retrievability, rating);

        double intervalDays = rating == Rating.AGAIN
                ? 10.0 / (24.0 * 60.0)
                : nextIntervalDays(stability);
        return new Schedule(now.plus(Duration.ofSeconds(Math.max(60, Math.round(intervalDays * 86400)))),
                difficulty, stability);
    }

    private double initialStability(Rating rating) {
        double grade = grade(rating);
        return Math.max(MIN_STABILITY, W[0] + W[1] * (grade - 1) + W[2] * Math.pow(grade - 1, 2)
                + W[3] * Math.pow(grade - 1, 3));
    }

    private double initialDifficulty(Rating rating) {
        return clamp(W[4] - Math.exp(W[5] * (grade(rating) - 1)) + 1, 1, 10);
    }

    private double nextDifficulty(double difficulty, Rating rating) {
        double delta = -W[6] * (grade(rating) - 3);
        double updated = difficulty + delta * (10 - difficulty) / 9;
        return clamp(W[7] * W[4] + (1 - W[7]) * updated, 1, 10);
    }

    private double nextRecallStability(double stability, double difficulty, double retrievability, Rating rating) {
        double hardPenalty = rating == Rating.HARD ? W[15] : 1;
        double easyBonus = rating == Rating.EASY ? W[16] : 1;
        double growth = Math.exp(W[8]) * (11 - difficulty) * Math.pow(stability, -W[9])
                * (Math.exp(W[10] * (1 - retrievability)) - 1) * hardPenalty * easyBonus;
        return Math.max(MIN_STABILITY, stability * (1 + growth));
    }

    private double nextForgetStability(double stability, double difficulty, double retrievability) {
        double next = W[11] * Math.pow(difficulty, -W[12])
                * (Math.pow(stability + 1, W[13]) - 1) * Math.exp(W[14] * (1 - retrievability));
        return Math.max(MIN_STABILITY, Math.min(stability, next));
    }

    private double nextIntervalDays(double stability) {
        return Math.min(MAX_INTERVAL_DAYS,
                Math.max(1, stability * Math.log(DESIRED_RETENTION) / Math.log(0.9)));
    }

    private Instant firstReviewDueAt(Rating rating, Instant now) {
        return switch (rating) {
            case AGAIN -> now.plusSeconds(60);
            case HARD -> now.plus(Duration.ofMinutes(6));
            case GOOD -> now.plus(Duration.ofDays(1));
            case EASY -> now.plus(Duration.ofDays(4));
        };
    }

    private double elapsedDays(Instant lastReviewedAt, Instant now) {
        if (lastReviewedAt == null || !now.isAfter(lastReviewedAt)) {
            return 0;
        }
        return Duration.between(lastReviewedAt, now).toSeconds() / 86400.0;
    }

    private int grade(Rating rating) {
        return switch (rating) {
            case AGAIN -> 1;
            case HARD -> 2;
            case GOOD -> 3;
            case EASY -> 4;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public String formatInterval(Instant dueAt, Instant now) {
        long seconds = Math.max(0, Duration.between(now, dueAt).getSeconds());
        if (seconds < 60) return "< 1m";
        if (seconds < 3600) return Math.max(1, Math.round(seconds / 60.0)) + "m";
        if (seconds < 86400) return Math.max(1, Math.round(seconds / 3600.0)) + "h";
        return String.format(Locale.ROOT, "%dd", Math.max(1, Math.round(seconds / 86400.0)));
    }

    @Value
    public static class Schedule {
        Instant dueAt;
        double difficulty;
        double stability;
    }
}

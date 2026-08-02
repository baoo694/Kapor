package com.kapor.analytics.service;

import com.kapor.analytics.model.LearningActivityEvent;
import com.kapor.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** Maintains the all-time counters displayed on the learner profile. */
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final MongoTemplate mongoTemplate;

    /**
     * Applies an already-persisted, idempotent learning event to the profile
     * counters. Mongo's $inc makes concurrent learning requests safe.
     */
    public void addActivity(String userId, LearningActivityEvent event) {
        if (event == null) return;

        Update update = new Update();
        boolean hasCounters = false;
        if (event.getMinutesStudied() > 0) {
            update.inc("stats.totalStudyMinutes", event.getMinutesStudied());
            hasCounters = true;
        }
        if (event.getCardsReviewed() > 0) {
            update.inc("stats.totalCardsReviewed", event.getCardsReviewed());
            hasCounters = true;
        }
        if (event.getRoleplaySessions() > 0) {
            update.inc("stats.totalRoleplaySessions", event.getRoleplaySessions());
            hasCounters = true;
        }
        if (event.getVideosWatched() > 0) {
            update.inc("stats.totalVideosWatched", event.getVideosWatched());
            hasCounters = true;
        }
        if (!hasCounters) return;

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(userId)),
                update,
                User.class);
    }
}

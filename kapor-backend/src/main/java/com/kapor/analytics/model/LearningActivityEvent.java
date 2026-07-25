package com.kapor.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable, idempotent record of one learning action. DailyActivity is a
 * denormalized dashboard projection; this collection prevents retries from
 * inflating that projection and keeps the original activity trace available.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "learning_activity_events")
@CompoundIndex(name = "userId_1_eventKey_1", def = "{'userId': 1, 'eventKey': 1}", unique = true)
public class LearningActivityEvent {

    @Id
    private String id;

    private String userId;
    private String eventKey;
    private String type;
    private Instant occurredAt;
    private LocalDate localDate;

    private int cardsReviewed;
    private int minutesStudied;
    private int roleplaySessions;
    private int lessonsCompleted;
    private int videosWatched;

    private Integer speakingScore;
    private Integer vocabularyScore;
    private Integer listeningScore;
    private Integer roleplayScore;
}

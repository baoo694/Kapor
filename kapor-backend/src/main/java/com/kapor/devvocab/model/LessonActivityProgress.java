package com.kapor.devvocab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lesson_activity_progress")
@CompoundIndex(name = "user_lesson_activity_unique", def = "{'userId': 1, 'lessonId': 1}", unique = true)
public class LessonActivityProgress {

    @Id
    private String id;

    private String userId;
    private String lessonId;
    private boolean studyCompleted;
    private Instant studyCompletedAt;
    private boolean quizPassed;
    private int bestQuizScore;
    private int quizAttempts;
    private int bestMatchAccuracy;
    private int matchingAttempts;

    @LastModifiedDate
    private Instant updatedAt;
}

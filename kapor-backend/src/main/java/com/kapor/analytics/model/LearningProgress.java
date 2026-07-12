package com.kapor.analytics.model;

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
@Document(collection = "learning_progress")
@CompoundIndex(name = "userId_1_topicId_1", def = "{'userId': 1, 'topicId': 1}", unique = true)
public class LearningProgress {

    @Id
    private String id;

    private String userId;
    private String domain; // "frontend", "backend", etc.
    private String topicId;

    @Builder.Default
    private int completedLessons = 0;
    
    private int totalLessons;
    
    @Builder.Default
    private double completionPercent = 0.0;

    private boolean isUnlocked;

    @LastModifiedDate
    private Instant lastAccessedAt;
}

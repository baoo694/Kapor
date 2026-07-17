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
@Document(collection = "flashcard_progress")
@CompoundIndex(
        name = "user_lesson_vocabulary_unique",
        def = "{'userId': 1, 'lessonId': 1, 'vocabularyId': 1}",
        unique = true)
public class FlashcardProgress {

    public enum Status {
        KNOWN,
        LEARNING
    }

    @Id
    private String id;

    private String userId;
    private String lessonId;
    private String vocabularyId;
    private Status status;

    @LastModifiedDate
    private Instant updatedAt;
}

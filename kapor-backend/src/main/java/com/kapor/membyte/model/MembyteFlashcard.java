package com.kapor.membyte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "membyte_flashcards")
@CompoundIndex(name = "user_lesson_vocabulary_saved_unique", def = "{'userId': 1, 'lessonId': 1, 'vocabularyId': 1}", unique = true)
public class MembyteFlashcard {

    @Id
    private String id;

    private String userId;
    private String deckId;
    private String lessonId;
    private String vocabularyId;

    // Vocabulary content is copied when it is starred, making the learner's saved card immutable.
    private String korean;
    private String pronunciation;
    private String vietnamese;
    private String english;
    private String definitionEn;
    private String exampleKo;
    private String grammarNote;
    private String context;
    private String codeSnippet;
    private String audioUrl;

    private boolean isNew;
    private Instant dueAt;
    private Instant lastReviewedAt;
    private int repetitions;
    private int lapses;
    private double difficulty;
    private double stability;
    private Instant createdAt;
}

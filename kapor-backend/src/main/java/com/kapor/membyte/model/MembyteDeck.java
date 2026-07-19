package com.kapor.membyte.model;

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
@Document(collection = "membyte_decks")
@CompoundIndex(name = "user_lesson_deck_unique", def = "{'userId': 1, 'lessonId': 1}", unique = true)
public class MembyteDeck {

    @Id
    private String id;

    private String userId;
    private String topicId;
    private String lessonId;
    private String domain;

    // These are snapshots so a learner's deck remains understandable even if an admin edits a lesson later.
    private String title;
    private String titleVi;

    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

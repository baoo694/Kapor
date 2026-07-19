package com.kapor.membyte.repository;

import com.kapor.membyte.model.MembyteFlashcard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembyteFlashcardRepository extends MongoRepository<MembyteFlashcard, String> {
    List<MembyteFlashcard> findByUserIdOrderByCreatedAtAsc(String userId);

    List<MembyteFlashcard> findByUserIdAndDeckIdOrderByCreatedAtAsc(String userId, String deckId);

    List<MembyteFlashcard> findByUserIdAndLessonId(String userId, String lessonId);

    Optional<MembyteFlashcard> findByUserIdAndLessonIdAndVocabularyId(
            String userId, String lessonId, String vocabularyId);
}

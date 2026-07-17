package com.kapor.devvocab.repository;

import com.kapor.devvocab.model.FlashcardProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardProgressRepository extends MongoRepository<FlashcardProgress, String> {

    List<FlashcardProgress> findByUserIdAndLessonId(String userId, String lessonId);

    Optional<FlashcardProgress> findByUserIdAndLessonIdAndVocabularyId(
            String userId,
            String lessonId,
            String vocabularyId);

    void deleteByUserIdAndLessonId(String userId, String lessonId);
}

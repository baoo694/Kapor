package com.kapor.devvocab.repository;

import com.kapor.devvocab.model.LessonActivityProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LessonActivityProgressRepository extends MongoRepository<LessonActivityProgress, String> {
    Optional<LessonActivityProgress> findByUserIdAndLessonId(String userId, String lessonId);
}

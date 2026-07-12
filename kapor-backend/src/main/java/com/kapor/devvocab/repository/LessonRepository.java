package com.kapor.devvocab.repository;

import com.kapor.devvocab.model.Lesson;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findByTopicIdOrderByOrderAsc(String topicId);
    long countByTopicId(String topicId);
}

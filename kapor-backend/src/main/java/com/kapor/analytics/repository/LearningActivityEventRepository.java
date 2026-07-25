package com.kapor.analytics.repository;

import com.kapor.analytics.model.LearningActivityEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningActivityEventRepository extends MongoRepository<LearningActivityEvent, String> {

    boolean existsByUserIdAndEventKey(String userId, String eventKey);
}

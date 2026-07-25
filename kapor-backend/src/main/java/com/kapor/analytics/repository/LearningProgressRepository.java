package com.kapor.analytics.repository;

import com.kapor.analytics.model.LearningProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningProgressRepository extends MongoRepository<LearningProgress, String> {

    List<LearningProgress> findByUserId(String userId);

    List<LearningProgress> findByUserIdAndDomain(String userId, String domain);

    Optional<LearningProgress> findByUserIdAndTopicId(String userId, String topicId);
}

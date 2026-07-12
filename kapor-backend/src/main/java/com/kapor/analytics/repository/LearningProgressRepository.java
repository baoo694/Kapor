package com.kapor.analytics.repository;

import com.kapor.analytics.model.LearningProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningProgressRepository extends MongoRepository<LearningProgress, String> {

    List<LearningProgress> findByUserId(String userId);

    List<LearningProgress> findByUserIdAndDomain(String userId, String domain);
}

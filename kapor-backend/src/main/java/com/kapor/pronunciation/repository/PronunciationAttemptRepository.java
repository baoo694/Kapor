package com.kapor.pronunciation.repository;

import com.kapor.pronunciation.model.PronunciationAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PronunciationAttemptRepository extends MongoRepository<PronunciationAttempt, String> {
    List<PronunciationAttempt> findByUserIdOrderByAttemptedAtDesc(String userId);
}

package com.kapor.pronunciation.repository;

import com.kapor.pronunciation.model.PronunciationAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.time.Instant;

public interface PronunciationAttemptRepository extends MongoRepository<PronunciationAttempt, String> {
    List<PronunciationAttempt> findByUserIdOrderByAttemptedAtDesc(String userId);
    List<PronunciationAttempt> findByUserIdAndExerciseIdOrderByAttemptedAtDesc(String userId, String exerciseId);
    List<PronunciationAttempt> findByExpiresAtBeforeAndAudioObjectKeyIsNotNull(Instant expiresAt);
}

package com.kapor.pronunciation.repository;

import com.kapor.pronunciation.model.PronunciationExercise;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PronunciationExerciseRepository extends MongoRepository<PronunciationExercise, String> {
    List<PronunciationExercise> findAllByOrderByOrderAsc();
}

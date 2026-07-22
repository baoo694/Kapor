package com.kapor.techtalk.repository;

import com.kapor.techtalk.model.TechTalkScenario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TechTalkScenarioRepository extends MongoRepository<TechTalkScenario, String> {
    List<TechTalkScenario> findByActiveTrue();
}

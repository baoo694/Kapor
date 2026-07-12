package com.kapor.devvocab.repository;

import com.kapor.devvocab.model.Topic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends MongoRepository<Topic, String> {
    List<Topic> findByDomainAndIsActiveTrueOrderByOrderAsc(String domain);
}

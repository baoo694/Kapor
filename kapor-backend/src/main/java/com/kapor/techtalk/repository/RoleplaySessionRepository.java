package com.kapor.techtalk.repository;

import com.kapor.techtalk.model.RoleplaySession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoleplaySessionRepository extends MongoRepository<RoleplaySession, String> {
    List<RoleplaySession> findByUserIdOrderByStartedAtDesc(String userId);
}

package com.kapor.techtalk.repository;

import com.kapor.techtalk.model.RoleplaySession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;

public interface RoleplaySessionRepository extends MongoRepository<RoleplaySession, String> {
    List<RoleplaySession> findByUserIdAndTestModeFalseOrderByStartedAtDesc(String userId);
    List<RoleplaySession> findByUserIdAndTestModeFalseOrderByStartedAtDesc(
            String userId, org.springframework.data.domain.Pageable pageable);
    Optional<RoleplaySession> findFirstByUserIdAndScenarioIdAndStatusOrderByStartedAtDesc(
            String userId, String scenarioId, String status);
    List<RoleplaySession> findByStatusAndLastActivityAtBefore(String status, Instant cutoff);
    List<RoleplaySession> findByTestModeTrueAndEndedAtBefore(Instant cutoff);
    boolean existsByScenarioId(String scenarioId);
}

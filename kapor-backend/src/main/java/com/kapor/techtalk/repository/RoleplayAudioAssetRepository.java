package com.kapor.techtalk.repository;

import com.kapor.techtalk.model.RoleplayAudioAsset;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoleplayAudioAssetRepository extends MongoRepository<RoleplayAudioAsset, String> {
    Optional<RoleplayAudioAsset> findByIdAndUserIdAndSessionId(String id, String userId, String sessionId);
    List<RoleplayAudioAsset> findByExpiresAtBefore(Instant cutoff);
}

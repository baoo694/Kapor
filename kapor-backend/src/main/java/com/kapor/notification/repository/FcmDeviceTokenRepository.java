package com.kapor.notification.repository;

import com.kapor.notification.model.FcmDeviceToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FcmDeviceTokenRepository extends MongoRepository<FcmDeviceToken, String> {
    Optional<FcmDeviceToken> findByToken(String token);
    List<FcmDeviceToken> findByUserIdAndEnabledTrue(String userId);
}

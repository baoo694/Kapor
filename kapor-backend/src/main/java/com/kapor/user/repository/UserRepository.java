package com.kapor.user.repository;

import com.kapor.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<User> findByProfileDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String displayName, String email, org.springframework.data.domain.Pageable pageable);
}

package com.kapor.admin.repository;

import com.kapor.admin.model.AdminPrompt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPromptRepository extends MongoRepository<AdminPrompt, String> {
    Optional<AdminPrompt> findFirstByKeyAndStatusOrderByPromptVersionDesc(String key, String status);
    List<AdminPrompt> findByKeyOrderByPromptVersionDesc(String key);
}

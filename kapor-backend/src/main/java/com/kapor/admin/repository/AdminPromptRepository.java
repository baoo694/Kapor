package com.kapor.admin.repository;

import com.kapor.admin.model.AdminPrompt;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdminPromptRepository extends MongoRepository<AdminPrompt, String> {
}

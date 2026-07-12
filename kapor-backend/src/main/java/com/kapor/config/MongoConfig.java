package com.kapor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Basic auditing is enabled via annotation
    // Custom converters (like YearMonth/Duration mapping if needed) would go here
}

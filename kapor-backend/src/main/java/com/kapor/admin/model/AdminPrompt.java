package com.kapor.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_prompts")
public class AdminPrompt {
    @Id private String id;
    private String key;
    private String name;
    private String description;
    private String content;
    private Integer promptVersion;
    private String status;
    private String updatedBy;
    @Builder.Default
    private List<String> requiredPlaceholders = new ArrayList<>();
    @Version
    private Long documentVersion;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}

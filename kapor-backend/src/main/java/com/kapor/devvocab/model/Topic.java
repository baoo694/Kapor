package com.kapor.devvocab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "topics")
public class Topic {

    @Id
    private String id;
    
    private String domain; // "frontend", "backend", "devops", etc.
    private String title;
    private String titleVi;
    
    private String description;
    
    private int order; // for sorting within domain
    
    private List<String> prerequisiteTopicIds; // For skill tree unlocking
    
    private List<String> tags;
    
    private boolean isActive;
}

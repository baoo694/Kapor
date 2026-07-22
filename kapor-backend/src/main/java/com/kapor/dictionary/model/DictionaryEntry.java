package com.kapor.dictionary.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dictionary_entries")
public class DictionaryEntry {
    @Id private String id;
    private String korean;
    private String pronunciation;
    private String vietnamese;
    private String english;
    private String domain;
    private String hanja;
    private String frequency;
    @Builder.Default private long searchCount = 0;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}

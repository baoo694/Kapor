package com.kapor.techtalk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roleplay_audio_assets")
public class RoleplayAudioAsset {
    @Id private String id;
    private String userId;
    private String sessionId;
    private String objectKey;
    private String contentType;
    private Long durationMs;
    private String transcript;
    private Double confidence;
    private Instant createdAt;
    private Instant expiresAt;
}

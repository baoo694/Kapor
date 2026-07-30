package com.kapor.techtalk.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoleplayTranscriptionDto {
    String audioId;
    String transcript;
    Double confidence;
    Long durationMs;
}

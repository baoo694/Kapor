package com.kapor.devvocab.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MatchingAttemptRequest {
    @Min(value = 0, message = "Completed pairs cannot be negative")
    private int completedPairs;

    @Min(value = 0, message = "Mistakes cannot be negative")
    private int mistakes;

    @Min(value = 0, message = "Duration cannot be negative")
    private int durationSeconds;
}

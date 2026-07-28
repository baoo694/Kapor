package com.kapor.devvocab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class QuizSubmissionRequest {
    @NotNull(message = "Quiz answers are required")
    private Map<String, String> answers;
}

package com.kapor.devvocab.dto;

import com.kapor.devvocab.model.FlashcardProgress;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FlashcardStatusRequest {

    @NotNull(message = "Flashcard status is required")
    private FlashcardProgress.Status status;
}

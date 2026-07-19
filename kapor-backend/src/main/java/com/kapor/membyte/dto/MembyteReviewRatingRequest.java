package com.kapor.membyte.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembyteReviewRatingRequest {

    public enum Rating {
        AGAIN, HARD, GOOD, EASY
    }

    @NotNull
    private String cardId;

    @NotNull
    private Rating rating;

    private Long responseTimeMs;
}

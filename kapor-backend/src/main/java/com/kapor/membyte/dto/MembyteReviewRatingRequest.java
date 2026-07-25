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

    /** Client-generated id used to make a retried review idempotent. */
    private String activityId;
}

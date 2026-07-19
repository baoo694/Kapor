package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MembyteReviewResultDto {
    Instant nextReview;
    String nextReviewLabel;
    double difficulty;
    double stability;
}

package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MembyteReviewSummaryDto {
    int totalDueCards;
    int decksWithDueCards;
}

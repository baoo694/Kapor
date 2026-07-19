package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MembyteDeckSummaryDto {
    String id;
    String title;
    String titleVi;
    String domain;
    int totalCards;
    int dueCards;
    int newCards;
}

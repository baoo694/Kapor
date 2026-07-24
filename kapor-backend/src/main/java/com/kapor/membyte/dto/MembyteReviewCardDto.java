package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class MembyteReviewCardDto {
    String id;
    String deckId;
    String korean;
    String pronunciation;
    String vietnamese;
    String english;
    String definitionEn;
    String exampleKo;
    String grammarNote;
    String context;
    String codeSnippet;
    String audioUrl;
    Map<String, String> nextReviewTimes;
}

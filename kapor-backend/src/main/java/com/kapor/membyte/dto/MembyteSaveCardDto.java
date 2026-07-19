package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MembyteSaveCardDto {
    String deckId;
    String cardId;
    String vocabularyId;
    boolean alreadySaved;
}

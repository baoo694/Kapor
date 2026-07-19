package com.kapor.membyte.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class MembyteSavedCardsDto {
    Set<String> vocabularyIds;
}

package com.kapor.devvocab.dto;

import com.kapor.devvocab.model.FlashcardProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardProgressDto {

    private String lessonId;
    private int totalCards;
    private int knownCards;
    private int learningCards;
    private Map<String, FlashcardProgress.Status> cardStatuses;
}

package com.kapor.summarizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarizerPreviewDto {
    private String sourceType;
    private String sourceUrl;
    private String title;
    private String sourceExcerpt;
    private List<SummarizerCardDto> cards;
}

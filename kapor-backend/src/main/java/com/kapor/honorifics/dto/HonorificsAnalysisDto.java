package com.kapor.honorifics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HonorificsAnalysisDto {
    private String currentLevel;
    private double confidence;
    /** Indicates whether the result came from deterministic rules or Gemini fallback. */
    private String analysisSource;
    private List<CorrectionDiffDto> corrections;
    private String transformedText;
}

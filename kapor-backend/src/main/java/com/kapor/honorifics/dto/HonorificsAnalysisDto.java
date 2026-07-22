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
    private List<CorrectionDiffDto> corrections;
    private String transformedText;
}

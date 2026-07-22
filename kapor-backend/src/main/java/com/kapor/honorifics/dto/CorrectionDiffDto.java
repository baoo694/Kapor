package com.kapor.honorifics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionDiffDto {
    private String original;
    private String corrected;
    private String type;
    private String explanation;
}

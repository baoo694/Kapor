package com.kapor.honorifics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HonorificsAnalyzeRequest {
    @NotBlank(message = "Text is required")
    @Size(max = 1500, message = "Text must not exceed 1500 characters")
    private String text;

    private String targetLevel = "hasipsio";
}

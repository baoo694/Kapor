package com.kapor.tts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KoreanTtsRequest(
        @NotBlank(message = "Từ tiếng Hàn không được để trống")
        @Size(max = 180, message = "Từ tiếng Hàn không được vượt quá 180 ký tự")
        String text) {
}

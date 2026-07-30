package com.kapor.tts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KoreanDialogueTtsRequest(
        @NotBlank(message = "Nội dung hội thoại tiếng Hàn không được để trống")
        @Size(max = 800, message = "Nội dung hội thoại tiếng Hàn không được vượt quá 800 ký tự")
        String text) {
}

package com.kapor.techtalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StreamRoleplayTurnRequest {
    @NotBlank(message = "Client turn id is required")
    @Size(max = 100, message = "Client turn id is too long")
    private String clientTurnId;

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String content;

    @Pattern(regexp = "text|voice", message = "Source must be text or voice")
    private String source = "text";

    @Size(max = 2000, message = "Transcript must not exceed 2000 characters")
    private String transcript;

    @Size(max = 100, message = "Audio id is too long")
    private String audioId;
}

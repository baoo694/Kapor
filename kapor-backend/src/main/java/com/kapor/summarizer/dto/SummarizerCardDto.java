package com.kapor.summarizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarizerCardDto {
    @NotBlank(message = "Mỗi thẻ cần có từ tiếng Hàn.") @Size(max = 120) private String korean;
    @Size(max = 160) private String pronunciation;
    @NotBlank(message = "Mỗi thẻ cần có nghĩa tiếng Việt.") @Size(max = 400) private String vietnamese;
    @Size(max = 400) private String english;
    @Size(max = 800) private String definitionEn;
    @Size(max = 800) private String exampleKo;
    @Size(max = 800) private String grammarNote;
    @Size(max = 1200) private String context;
}

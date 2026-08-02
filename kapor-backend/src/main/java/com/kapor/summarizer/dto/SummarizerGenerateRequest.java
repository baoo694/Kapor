package com.kapor.summarizer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SummarizerGenerateRequest {
    @NotBlank(message = "Hãy dán URL hoặc nội dung bài viết tiếng Hàn.")
    @Size(max = 16000, message = "Nội dung nguồn quá dài.")
    private String input;

    @Min(value = 3, message = "Cần tạo ít nhất 3 thẻ.")
    @Max(value = 12, message = "Có thể tạo tối đa 12 thẻ mỗi lần.")
    private Integer maxCards = 8;
}

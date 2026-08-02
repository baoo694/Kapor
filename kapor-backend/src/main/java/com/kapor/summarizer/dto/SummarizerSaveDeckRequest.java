package com.kapor.summarizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SummarizerSaveDeckRequest {
    @Size(max = 120, message = "Tên deck quá dài.")
    private String title;
    @Size(max = 2048, message = "URL nguồn không hợp lệ.")
    private String sourceUrl;
    @Size(max = 160, message = "Tiêu đề nguồn quá dài.")
    private String sourceTitle;
    @Size(max = 16000, message = "Nguồn quá dài.")
    private String sourceExcerpt;
    @NotEmpty(message = "Hãy chọn ít nhất một thẻ để lưu.")
    @Size(max = 12, message = "Có thể lưu tối đa 12 thẻ mỗi lần.")
    private List<@Valid SummarizerCardDto> cards;
}

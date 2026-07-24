package com.kapor.video.dto;

import com.kapor.video.model.Video;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Korean draft lines to translate into Vietnamese with the configured Gemini model. */
@Data
public class SubtitleTranslateRequest {
    @NotNull(message = "Korean subtitles are required")
    @NotEmpty(message = "At least one Korean subtitle is required")
    @Valid
    private List<Video.SubtitleLine> koreanSubtitles = new ArrayList<>();
}

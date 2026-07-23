package com.kapor.video.dto;

import com.kapor.video.model.Video;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Korean source lines can be tokenized before a Vietnamese translation is available. */
@Data
public class SubtitleTokenizeRequest {
    @NotNull(message = "Korean subtitles are required")
    @Valid
    private List<Video.SubtitleLine> koreanSubtitles = new ArrayList<>();
}

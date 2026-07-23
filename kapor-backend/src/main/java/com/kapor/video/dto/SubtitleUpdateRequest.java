package com.kapor.video.dto;

import com.kapor.video.model.Video;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Payload used by the admin subtitle editor.  Each Korean line has one Vietnamese pair. */
@Data
public class SubtitleUpdateRequest {
    @NotNull(message = "Korean subtitles are required")
    @Valid
    private List<Video.SubtitleLine> koreanSubtitles = new ArrayList<>();

    @NotNull(message = "Vietnamese subtitles are required")
    @Valid
    private List<Video.SubtitleLine> vietnameseSubtitles = new ArrayList<>();
}

package com.kapor.video.dto;

import com.kapor.video.model.Video;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class VideoDto {
    private String id;
    private String title;
    private String youtubeUrl;
    private Instant createdAt;
    
    public static VideoDto fromEntity(Video video) {
        return VideoDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .youtubeUrl(video.getYoutubeUrl())
                .createdAt(video.getCreatedAt())
                .build();
    }
}

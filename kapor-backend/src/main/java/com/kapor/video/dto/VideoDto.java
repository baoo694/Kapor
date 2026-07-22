package com.kapor.video.dto;

import com.kapor.video.model.Video;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VideoDto {
    private String id;
    private String title;
    private String titleVi;
    private String youtubeUrl;
    private String youtubeVideoId;
    private String thumbnailUrl;
    private String domain;
    private String difficulty;
    private Integer durationSeconds;
    @Builder.Default
    private List<Video.SubtitleLine> koreanSubtitles = new ArrayList<>();
    @Builder.Default
    private List<Video.SubtitleLine> vietnameseSubtitles = new ArrayList<>();
    @Builder.Default
    private List<Video.QuizMarker> quizMarkers = new ArrayList<>();
    private Instant createdAt;
    
    public static VideoDto fromEntity(Video video) {
        return VideoDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .titleVi(video.getTitleVi())
                .youtubeUrl(video.getYoutubeUrl())
                .youtubeVideoId(video.getYoutubeVideoId())
                .thumbnailUrl(video.getThumbnailUrl())
                .domain(video.getDomain())
                .difficulty(video.getDifficulty())
                .durationSeconds(video.getDurationSeconds())
                .koreanSubtitles(video.getKoreanSubtitles())
                .vietnameseSubtitles(video.getVietnameseSubtitles())
                .quizMarkers(video.getQuizMarkers())
                .createdAt(video.getCreatedAt())
                .build();
    }
}

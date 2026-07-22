package com.kapor.video.service;

import com.kapor.video.dto.VideoDto;
import com.kapor.video.model.Video;
import com.kapor.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    public List<VideoDto> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(VideoDto::fromEntity)
                .collect(Collectors.toList());
    }

    public VideoDto createVideo(VideoDto dto) {
        Video video = Video.builder()
                .title(dto.getTitle())
                .titleVi(dto.getTitleVi())
                .youtubeUrl(dto.getYoutubeUrl())
                .youtubeVideoId(nonBlank(dto.getYoutubeVideoId(), extractYoutubeId(dto.getYoutubeUrl())))
                .thumbnailUrl(dto.getThumbnailUrl())
                .domain(dto.getDomain())
                .difficulty(dto.getDifficulty())
                .durationSeconds(dto.getDurationSeconds())
                .koreanSubtitles(dto.getKoreanSubtitles())
                .vietnameseSubtitles(dto.getVietnameseSubtitles())
                .quizMarkers(dto.getQuizMarkers())
                .build();
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public VideoDto updateVideo(String id, VideoDto dto) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setTitle(dto.getTitle());
        video.setTitleVi(dto.getTitleVi());
        video.setYoutubeUrl(dto.getYoutubeUrl());
        video.setYoutubeVideoId(nonBlank(dto.getYoutubeVideoId(), extractYoutubeId(dto.getYoutubeUrl())));
        video.setThumbnailUrl(dto.getThumbnailUrl());
        video.setDomain(dto.getDomain());
        video.setDifficulty(dto.getDifficulty());
        video.setDurationSeconds(dto.getDurationSeconds());
        video.setKoreanSubtitles(dto.getKoreanSubtitles());
        video.setVietnameseSubtitles(dto.getVietnameseSubtitles());
        video.setQuizMarkers(dto.getQuizMarkers());
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public void deleteVideo(String id) {
        videoRepository.deleteById(id);
    }

    public VideoDto getVideo(String id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new com.kapor.common.exception.ResourceNotFoundException("Video", "id", id));
        return VideoDto.fromEntity(video);
    }

    public List<VideoDto> getVideos(String domain) {
        return videoRepository.findAll().stream()
                .filter(video -> domain == null || domain.isBlank() || domain.equalsIgnoreCase(video.getDomain()))
                .map(VideoDto::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean answerQuiz(String videoId, String quizId, int answer) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new com.kapor.common.exception.ResourceNotFoundException("Video", "id", videoId));
        Video.QuizMarker marker = video.getQuizMarkers().stream()
                .filter(item -> quizId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new com.kapor.common.exception.ResourceNotFoundException("Quiz", "id", quizId));
        return marker.getCorrectAnswer() != null && marker.getCorrectAnswer() == answer;
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) return null;
        String value = url.trim();
        if (!value.contains("/")) return value;
        int query = value.indexOf("v=");
        if (query >= 0) {
            String id = value.substring(query + 2);
            return id.split("[&#?]")[0];
        }
        String[] parts = value.split("/");
        return parts.length == 0 ? null : parts[parts.length - 1].split("[?#]")[0];
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

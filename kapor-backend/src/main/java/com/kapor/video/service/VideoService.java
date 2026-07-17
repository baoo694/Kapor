package com.kapor.video.service;

import com.kapor.video.dto.VideoDto;
import com.kapor.video.model.Video;
import com.kapor.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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
                .youtubeUrl(dto.getYoutubeUrl())
                .build();
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public VideoDto updateVideo(String id, VideoDto dto) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setTitle(dto.getTitle());
        video.setYoutubeUrl(dto.getYoutubeUrl());
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public void deleteVideo(String id) {
        videoRepository.deleteById(id);
    }
}

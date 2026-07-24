package com.kapor.admin.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.video.dto.VideoDto;
import com.kapor.video.dto.SubtitleUpdateRequest;
import com.kapor.video.dto.SubtitleTokenizeRequest;
import com.kapor.video.dto.SubtitleAiAnalyzeRequest;
import com.kapor.video.dto.SubtitleTranslateRequest;
import jakarta.validation.Valid;
import com.kapor.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoDto>>> getVideos() {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getAllVideos()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VideoDto>> createVideo(@RequestBody VideoDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.createVideo(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDto>> updateVideo(@PathVariable String id, @RequestBody VideoDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.updateVideo(id, dto)));
    }

    @PutMapping("/{id}/subtitles")
    public ResponseEntity<ApiResponse<VideoDto>> updateSubtitles(
            @PathVariable String id, @Valid @RequestBody SubtitleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.updateSubtitles(id, request)));
    }

    @PostMapping("/{id}/subtitles/tokenize")
    public ResponseEntity<ApiResponse<VideoDto>> tokenizeSubtitles(
            @PathVariable String id, @Valid @RequestBody SubtitleTokenizeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.tokenizeKoreanSubtitles(id, request)));
    }

    @PostMapping("/{id}/subtitles/ai-tokenize")
    public ResponseEntity<ApiResponse<VideoDto>> tokenizeSubtitlesWithAi(
            @PathVariable String id, @Valid @RequestBody SubtitleTokenizeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.tokenizeKoreanSubtitlesWithAi(id, request)));
    }

    @PostMapping("/{id}/subtitles/translate")
    public ResponseEntity<ApiResponse<VideoDto>> translateSubtitles(
            @PathVariable String id, @Valid @RequestBody SubtitleTranslateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.translateSubtitlesWithAi(id, request)));
    }

    @PostMapping("/{id}/subtitles/ai-analyze")
    public ResponseEntity<ApiResponse<VideoDto>> analyzeSubtitlesWithAi(
            @PathVariable String id, @Valid @RequestBody SubtitleAiAnalyzeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.analyzeSubtitlesWithAi(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable String id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Video deleted successfully"));
    }
}

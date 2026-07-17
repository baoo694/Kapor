package com.kapor.admin.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.video.dto.VideoDto;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable String id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Video deleted successfully"));
    }
}

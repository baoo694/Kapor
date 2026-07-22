package com.kapor.video.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.video.dto.VideoDto;
import com.kapor.video.service.VideoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoDto>>> getVideos(@RequestParam(required = false) String domain) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideos(domain)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDto>> getVideo(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideo(id)));
    }

    @GetMapping("/{id}/subtitles")
    public ResponseEntity<ApiResponse<VideoDto>> getSubtitles(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideo(id)));
    }

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<ApiResponse<List<?>>> getQuizzes(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideo(id).getQuizMarkers()));
    }

    @PostMapping("/{videoId}/quiz/{quizId}/answer")
    public ResponseEntity<ApiResponse<QuizAnswerResponse>> answerQuiz(
            @PathVariable String videoId,
            @PathVariable String quizId,
            @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(new QuizAnswerResponse(
                videoService.answerQuiz(videoId, quizId, request.getAnswer()))));
    }

    @Data
    public static class QuizAnswerRequest { private int answer; }
    public record QuizAnswerResponse(boolean correct) { }
}

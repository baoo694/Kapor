package com.kapor.video.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.video.dto.VideoDto;
import com.kapor.video.service.VideoService;
import com.kapor.analytics.service.ActivityTrackingService;
import com.kapor.auth.security.CustomUserDetails;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
    private final ActivityTrackingService activityTrackingService;

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
            @RequestBody QuizAnswerRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Timezone-Offset-Minutes", required = false) Integer timezoneOffsetMinutes) {
        boolean correct = videoService.answerQuiz(videoId, quizId, request.getAnswer());
        String userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("video-quiz:" + videoId + ":" + quizId)
                .type("video_quiz")
                .listeningScore(correct ? 100 : 0)
                .build(), timezoneOffsetMinutes);
        return ResponseEntity.ok(ApiResponse.ok(new QuizAnswerResponse(
                correct)));
    }

    @PostMapping("/{videoId}/complete")
    public ResponseEntity<ApiResponse<VideoCompletionResponse>> completeVideo(
            @PathVariable String videoId,
            @Valid @RequestBody VideoCompletionRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Timezone-Offset-Minutes", required = false) Integer timezoneOffsetMinutes) {
        int watchedSeconds = videoService.validateCompletion(videoId, request.getWatchedSeconds());
        String userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("video-complete:" + videoId)
                .type("video_complete")
                .videosWatched(1)
                .minutesStudied(Math.max(1, (int) Math.ceil(watchedSeconds / 60.0)))
                .listeningScore(100)
                .build(), timezoneOffsetMinutes);
        return ResponseEntity.ok(ApiResponse.ok(new VideoCompletionResponse(true)));
    }

    @Data
    public static class QuizAnswerRequest { private int answer; }
    public record QuizAnswerResponse(boolean correct) { }

    @Data
    public static class VideoCompletionRequest {
        @NotNull(message = "Thời gian đã xem là bắt buộc")
        @Min(value = 0, message = "Thời gian đã xem không hợp lệ")
        private Integer watchedSeconds;
    }
    public record VideoCompletionResponse(boolean completed) { }
}

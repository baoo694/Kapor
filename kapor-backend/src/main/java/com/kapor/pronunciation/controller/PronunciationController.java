package com.kapor.pronunciation.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.pronunciation.dto.PronunciationEvaluationDto;
import com.kapor.pronunciation.dto.PronunciationAttemptDto;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.service.PronunciationService;
import com.kapor.analytics.service.ActivityTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pronunciation")
@RequiredArgsConstructor
public class PronunciationController {
    private final PronunciationService pronunciationService;
    private final ActivityTrackingService activityTrackingService;

    @GetMapping("/exercises")
    public ResponseEntity<ApiResponse<List<PronunciationExercise>>> exercises() {
        return ResponseEntity.ok(ApiResponse.ok(pronunciationService.exercises()));
    }

    @GetMapping("/exercises/{id}")
    public ResponseEntity<ApiResponse<PronunciationExercise>> exercise(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(pronunciationService.exercise(id)));
    }

    @PostMapping(value = "/evaluate", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PronunciationEvaluationDto>> evaluate(
            @RequestPart MultipartFile audioFile, @RequestParam String exerciseId,
            @RequestParam(defaultValue = "0") int sentenceIndex, Authentication authentication,
            @RequestHeader(value = "X-Timezone-Offset-Minutes", required = false) Integer timezoneOffsetMinutes) throws IOException {
        if (audioFile.isEmpty()) throw new IllegalArgumentException("Audio file is required");
        if (audioFile.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("Bản ghi không được vượt quá 2 MB");
        String contentType = audioFile.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("audio/pcm") && !contentType.equalsIgnoreCase("audio/l16")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ bản ghi PCM 16-bit, 16 kHz từ ứng dụng.");
        }
        String userId = userId(authentication);
        PronunciationEvaluationDto result = pronunciationService.evaluate(
                userId, exerciseId, sentenceIndex, audioFile.getBytes());
        activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("pronunciation-attempt:" + result.getAttemptId())
                .type("pronunciation_attempt")
                .build(), timezoneOffsetMinutes);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PronunciationAttemptDto>>> history(
            @RequestParam(required = false) String exerciseId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(pronunciationService.history(userId(authentication), exerciseId)));
    }

    @GetMapping(value = "/attempts/{id}/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> attemptAudio(@PathVariable String id, Authentication authentication) {
        var audio = pronunciationService.attemptAudio(userId(authentication), id);
        return ResponseEntity.ok().contentType(MediaType.valueOf(audio.contentType()))
                .contentLength(audio.bytes().length).body(audio.bytes());
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

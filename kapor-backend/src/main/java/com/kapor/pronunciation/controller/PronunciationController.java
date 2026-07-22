package com.kapor.pronunciation.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.pronunciation.dto.PronunciationEvaluationDto;
import com.kapor.pronunciation.model.PronunciationAttempt;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.service.PronunciationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
            @RequestParam(defaultValue = "0") int sentenceIndex, Authentication authentication) throws IOException {
        if (audioFile.isEmpty()) throw new IllegalArgumentException("Audio file is required");
        if (audioFile.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Audio must not exceed 10 MB");
        return ResponseEntity.ok(ApiResponse.ok(pronunciationService.evaluate(
                userId(authentication), exerciseId, sentenceIndex, audioFile.getBytes())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PronunciationAttempt>>> history(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(pronunciationService.history(userId(authentication))));
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

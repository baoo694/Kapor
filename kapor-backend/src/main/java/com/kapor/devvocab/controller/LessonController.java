package com.kapor.devvocab.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.devvocab.dto.FlashcardProgressDto;
import com.kapor.devvocab.dto.FlashcardStatusRequest;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.service.FlashcardProgressService;
import com.kapor.auth.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonRepository lessonRepository;
    private final FlashcardProgressService flashcardProgressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessonsByTopic(@RequestParam String topicId) {
        List<Lesson> lessons = lessonRepository.findByTopicIdOrderByOrderAsc(topicId);
        return ResponseEntity.ok(ApiResponse.ok(lessons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lesson>> getLessonDetail(@PathVariable String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        return ResponseEntity.ok(ApiResponse.ok(lesson));
    }

    @GetMapping("/{id}/flashcards/progress")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> getFlashcardProgress(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                flashcardProgressService.getProgress(getUserId(authentication), id)));
    }

    @PutMapping("/{id}/flashcards/{vocabularyId}")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> updateFlashcardStatus(
            @PathVariable String id,
            @PathVariable String vocabularyId,
            @Valid @RequestBody FlashcardStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                flashcardProgressService.updateStatus(
                        getUserId(authentication),
                        id,
                        vocabularyId,
                        request.getStatus()),
                "Flashcard progress saved"));
    }

    @DeleteMapping("/{id}/flashcards/progress")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> resetFlashcardProgress(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                flashcardProgressService.resetProgress(getUserId(authentication), id),
                "Flashcard progress reset"));
    }

    private String getUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

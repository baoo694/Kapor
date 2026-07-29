package com.kapor.devvocab.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.devvocab.dto.FlashcardProgressDto;
import com.kapor.devvocab.dto.FlashcardStatusRequest;
import com.kapor.devvocab.dto.LessonActivityProgressDto;
import com.kapor.devvocab.dto.MatchingAttemptRequest;
import com.kapor.devvocab.dto.QuizResultDto;
import com.kapor.devvocab.dto.QuizSubmissionRequest;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.service.FlashcardProgressService;
import com.kapor.devvocab.service.LessonActivityProgressService;
import com.kapor.analytics.service.ActivityTrackingService;
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
    private final LessonActivityProgressService lessonActivityProgressService;
    private final ActivityTrackingService activityTrackingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessonsByTopic(@RequestParam String topicId) {
        List<Lesson> lessons = lessonRepository.findByTopicIdOrderByOrderAsc(topicId);
        return ResponseEntity.ok(ApiResponse.ok(lessons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lesson>> getLessonDetail(
            @PathVariable String id,
            Authentication authentication) {
        Lesson lesson = flashcardProgressService.getUnlockedLesson(getUserId(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok(lesson));
    }

    @GetMapping("/{id}/flashcards/progress")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> getFlashcardProgress(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                flashcardProgressService.getProgress(getUserId(authentication), id)));
    }

    @GetMapping("/{id}/activity-progress")
    public ResponseEntity<ApiResponse<LessonActivityProgressDto>> getActivityProgress(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                lessonActivityProgressService.getProgress(getUserId(authentication), id)));
    }

    @PostMapping("/{id}/study/complete")
    public ResponseEntity<ApiResponse<LessonActivityProgressDto>> completeStudy(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                lessonActivityProgressService.completeStudy(getUserId(authentication), id),
                "Đã hoàn thành phần Học"));
    }

    @PostMapping("/{id}/quiz/attempts")
    public ResponseEntity<ApiResponse<QuizResultDto>> submitQuiz(
            @PathVariable String id,
            @Valid @RequestBody QuizSubmissionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                lessonActivityProgressService.submitQuiz(getUserId(authentication), id, request)));
    }

    @PostMapping("/{id}/matching/attempts")
    public ResponseEntity<ApiResponse<LessonActivityProgressDto>> recordMatchingAttempt(
            @PathVariable String id,
            @Valid @RequestBody MatchingAttemptRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                lessonActivityProgressService.recordMatchingAttempt(getUserId(authentication), id, request)));
    }

    @PutMapping("/{id}/flashcards/{vocabularyId}")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> updateFlashcardStatus(
            @PathVariable String id,
            @PathVariable String vocabularyId,
            @Valid @RequestBody FlashcardStatusRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Timezone-Offset-Minutes", required = false) Integer timezoneOffsetMinutes) {
        String userId = getUserId(authentication);
        FlashcardProgressDto progress = flashcardProgressService.updateStatus(
                userId,
                id,
                vocabularyId,
                request.getStatus());
        activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                .eventKey("devvocab-card:" + id + ":" + vocabularyId)
                .type("devvocab_card")
                .cardsReviewed(1)
                .vocabularyScore(request.getStatus() == com.kapor.devvocab.model.FlashcardProgress.Status.KNOWN ? 100 : 50)
                .build(), timezoneOffsetMinutes);
        if (progress.getTotalCards() > 0 && progress.getKnownCards() == progress.getTotalCards()) {
            activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                    .eventKey("devvocab-lesson-complete:" + id)
                    .type("devvocab_lesson_complete")
                    .lessonsCompleted(1)
                    .build(), timezoneOffsetMinutes);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                progress,
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

    @DeleteMapping("/{id}/flashcards/{vocabularyId}")
    public ResponseEntity<ApiResponse<FlashcardProgressDto>> resetFlashcardStatus(
            @PathVariable String id,
            @PathVariable String vocabularyId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                flashcardProgressService.resetCardProgress(
                        getUserId(authentication), id, vocabularyId),
                "Flashcard status reset"));
    }

    private String getUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

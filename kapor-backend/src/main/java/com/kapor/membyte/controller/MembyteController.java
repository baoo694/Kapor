package com.kapor.membyte.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.membyte.dto.MembyteDeckSummaryDto;
import com.kapor.membyte.dto.MembyteReviewCardDto;
import com.kapor.membyte.dto.MembyteReviewRatingRequest;
import com.kapor.membyte.dto.MembyteReviewResultDto;
import com.kapor.membyte.dto.MembyteReviewSummaryDto;
import com.kapor.membyte.dto.MembyteSaveCardDto;
import com.kapor.membyte.dto.MembyteSaveVideoTokenRequest;
import com.kapor.membyte.dto.MembyteSavedCardsDto;
import com.kapor.membyte.service.MembyteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membyte")
@RequiredArgsConstructor
public class MembyteController {

    private final MembyteService membyteService;

    @PostMapping("/lessons/{lessonId}/flashcards/{vocabularyId}")
    public ResponseEntity<ApiResponse<MembyteSaveCardDto>> saveVocabulary(
            @PathVariable String lessonId,
            @PathVariable String vocabularyId,
            Authentication authentication) {
        MembyteSaveCardDto result = membyteService.saveVocabulary(userId(authentication), lessonId, vocabularyId);
        String message = result.isAlreadySaved() ? "Flashcard đã có trong MemByte" : "Đã thêm flashcard vào MemByte";
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }

    @PostMapping("/videos/{videoId}/flashcards")
    public ResponseEntity<ApiResponse<MembyteSaveCardDto>> saveVideoToken(
            @PathVariable String videoId,
            @Valid @RequestBody MembyteSaveVideoTokenRequest request,
            Authentication authentication) {
        MembyteSaveCardDto result = membyteService.saveVideoToken(
                userId(authentication), videoId, request.getSurface());
        String message = result.isAlreadySaved()
                ? "Flashcard đã có trong bộ thẻ video"
                : "Đã thêm flashcard vào bộ thẻ video";
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }

    @GetMapping("/lessons/{lessonId}/saved-flashcards")
    public ResponseEntity<ApiResponse<MembyteSavedCardsDto>> getSavedVocabularyIds(
            @PathVariable String lessonId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                membyteService.getSavedVocabularyIds(userId(authentication), lessonId)));
    }

    @GetMapping("/decks")
    public ResponseEntity<ApiResponse<List<MembyteDeckSummaryDto>>> getDecks(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(membyteService.getDecks(userId(authentication))));
    }

    @GetMapping("/review/summary")
    public ResponseEntity<ApiResponse<MembyteReviewSummaryDto>> getReviewSummary(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(membyteService.getReviewSummary(userId(authentication))));
    }

    @GetMapping("/review/cards")
    public ResponseEntity<ApiResponse<List<MembyteReviewCardDto>>> getReviewCards(
            @RequestParam(required = false) String deckId,
            @RequestParam(defaultValue = "false") boolean includeNew,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                membyteService.getReviewCards(userId(authentication), deckId, includeNew, limit)));
    }

    @PostMapping("/review/rate")
    public ResponseEntity<ApiResponse<MembyteReviewResultDto>> rateCard(
            @Valid @RequestBody MembyteReviewRatingRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                membyteService.rateCard(userId(authentication), request), "Đã lên lịch ôn FSRS"));
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

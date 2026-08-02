package com.kapor.summarizer.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.summarizer.dto.SummarizerGenerateRequest;
import com.kapor.summarizer.dto.SummarizerPreviewDto;
import com.kapor.summarizer.dto.SummarizerSaveDeckDto;
import com.kapor.summarizer.dto.SummarizerSaveDeckRequest;
import com.kapor.summarizer.service.SummarizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summarizer")
@RequiredArgsConstructor
public class SummarizerController {
    private final SummarizerService summarizerService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<SummarizerPreviewDto>> generate(@Valid @RequestBody SummarizerGenerateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(summarizerService.generate(userId(authentication), request.getInput(), request.getMaxCards()), "Đã tạo bản nháp flashcard"));
    }

    @PostMapping("/decks")
    public ResponseEntity<ApiResponse<SummarizerSaveDeckDto>> saveDeck(@Valid @RequestBody SummarizerSaveDeckRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(summarizerService.saveDeck(userId(authentication), request), "Đã lưu flashcard vào MemByte"));
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

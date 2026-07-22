package com.kapor.honorifics.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import com.kapor.honorifics.dto.HonorificsAnalyzeRequest;
import com.kapor.honorifics.service.HonorificsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/honorifics")
@RequiredArgsConstructor
public class HonorificsController {
    private final HonorificsService honorificsService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<HonorificsAnalysisDto>> analyze(
            @Valid @RequestBody HonorificsAnalyzeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                honorificsService.analyze(request.getText(), request.getTargetLevel())));
    }

    @PostMapping("/transform")
    public ResponseEntity<ApiResponse<String>> transform(
            @Valid @RequestBody HonorificsAnalyzeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                honorificsService.transform(request.getText(), request.getTargetLevel())));
    }
}

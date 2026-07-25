package com.kapor.tts;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.tts.dto.KoreanTtsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {
    private final GeminiTtsService geminiTtsService;

    @PostMapping(value = "/korean", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "audio/wav")
    public ResponseEntity<byte[]> synthesizeKorean(
            @Valid @RequestBody KoreanTtsRequest request,
            Authentication authentication) {
        String userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        byte[] audio = geminiTtsService.synthesizeKorean(userId, request.text());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/wav"))
                .contentLength(audio.length)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .body(audio);
    }
}

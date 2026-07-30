package com.kapor.techtalk.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.dto.RoleplayHistoryDto;
import com.kapor.techtalk.dto.RoleplayStreamEvent;
import com.kapor.techtalk.dto.RoleplayTranscriptionDto;
import com.kapor.techtalk.dto.SendRoleplayMessageRequest;
import com.kapor.techtalk.dto.StartRoleplayRequest;
import com.kapor.techtalk.dto.StreamRoleplayTurnRequest;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.service.RoleplayService;
import com.kapor.techtalk.service.RoleplaySpeechService;
import com.kapor.analytics.service.ActivityTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/roleplay")
@RequiredArgsConstructor
public class RoleplayController {
    private final RoleplayService roleplayService;
    private final RoleplaySpeechService speechService;
    private final ActivityTrackingService activityTrackingService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<RoleplaySession>> start(
            @RequestBody(required = false) StartRoleplayRequest request,
            @RequestParam(required = false) String scenarioId,
            Authentication authentication) {
        String requestedScenario = request != null && request.getScenarioId() != null
                ? request.getScenarioId()
                : scenarioId;
        if (requestedScenario == null || requestedScenario.isBlank()) {
            throw new IllegalArgumentException("Scenario id is required");
        }
        boolean testMode = request != null && request.isTestMode() && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return ResponseEntity.ok(ApiResponse.ok(
                roleplayService.start(userId(authentication), requestedScenario, testMode)));
    }

    @PostMapping("/{sessionId}/send")
    public ResponseEntity<ApiResponse<RoleplaySession>> send(@PathVariable String sessionId,
            @Valid @RequestBody SendRoleplayMessageRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.send(userId(authentication), sessionId, request.getContent())));
    }

    @PostMapping(value = "/{sessionId}/turns/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RoleplayStreamEvent>> streamTurn(
            @PathVariable String sessionId,
            @Valid @RequestBody StreamRoleplayTurnRequest request,
            Authentication authentication) {
        return roleplayService.streamTurn(userId(authentication), sessionId, request)
                .map(event -> ServerSentEvent.<RoleplayStreamEvent>builder(event)
                        .id(event.getTurnId() + ":" + UUID.randomUUID())
                        .event(event.getType())
                        .build());
    }

    @PostMapping("/{sessionId}/hint")
    public ResponseEntity<ApiResponse<RoleplayHintDto>> hint(@PathVariable String sessionId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.hint(userId(authentication), sessionId)));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<RoleplaySession>> end(
            @PathVariable String sessionId,
            Authentication authentication,
            @RequestHeader(value = "X-Timezone-Offset-Minutes", required = false) Integer timezoneOffsetMinutes) {
        String userId = userId(authentication);
        RoleplaySession session = roleplayService.end(userId, sessionId);
        int elapsedMinutes = session.getStartedAt() == null || session.getEndedAt() == null
                ? 0
                : Math.max(1, (int) Math.ceil(Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds() / 60.0));
        Integer score = session.getFinalEvaluation() == null ? null : session.getFinalEvaluation().getOverallScore();
        if (!session.isTestMode()) {
            activityTrackingService.track(userId, ActivityTrackingService.ActivityUpdate.builder()
                    .eventKey("roleplay-complete:" + session.getId())
                    .type("roleplay_complete")
                    .roleplaySessions(1)
                    .minutesStudied(elapsedMinutes)
                    .roleplayScore(score)
                    .build(), timezoneOffsetMinutes);
        }
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @PostMapping("/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<RoleplaySession>> abandon(
            @PathVariable String sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.abandon(userId(authentication), sessionId)));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<RoleplaySession>> detail(
            @PathVariable String sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.detail(userId(authentication), sessionId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<RoleplaySession>>> history(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.history(userId(authentication))));
    }

    @GetMapping("/history/page")
    public ResponseEntity<ApiResponse<RoleplayHistoryDto>> historyPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.history(userId(authentication), page, size)));
    }

    @PostMapping(value = "/{sessionId}/audio/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoleplayTranscriptionDto>> transcribe(
            @PathVariable String sessionId,
            @RequestPart("audioFile") MultipartFile audioFile,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(speechService.transcribe(
                userId(authentication), sessionId, audioFile.getBytes(), audioFile.getContentType())));
    }

    @GetMapping(value = "/{sessionId}/audio/{audioId}", produces = "audio/wav")
    public ResponseEntity<byte[]> audio(
            @PathVariable String sessionId,
            @PathVariable String audioId,
            Authentication authentication) {
        byte[] audio = speechService.audio(userId(authentication), sessionId, audioId);
        return ResponseEntity.ok().contentType(MediaType.valueOf("audio/wav"))
                .cacheControl(CacheControl.noStore())
                .contentLength(audio.length).body(audio);
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

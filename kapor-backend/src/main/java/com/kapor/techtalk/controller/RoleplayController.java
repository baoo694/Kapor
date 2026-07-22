package com.kapor.techtalk.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.dto.SendRoleplayMessageRequest;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.service.RoleplayService;
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
@RequestMapping("/api/roleplay")
@RequiredArgsConstructor
public class RoleplayController {
    private final RoleplayService roleplayService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<RoleplaySession>> start(@RequestParam String scenarioId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.start(userId(authentication), scenarioId)));
    }

    @PostMapping("/{sessionId}/send")
    public ResponseEntity<ApiResponse<RoleplaySession>> send(@PathVariable String sessionId,
            @Valid @RequestBody SendRoleplayMessageRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.send(userId(authentication), sessionId, request.getContent())));
    }

    @PostMapping("/{sessionId}/hint")
    public ResponseEntity<ApiResponse<RoleplayHintDto>> hint(@PathVariable String sessionId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.hint(userId(authentication), sessionId)));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<RoleplaySession>> end(@PathVariable String sessionId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.end(userId(authentication), sessionId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<RoleplaySession>>> history(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.history(userId(authentication))));
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

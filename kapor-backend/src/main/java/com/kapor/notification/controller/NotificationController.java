package com.kapor.notification.controller;

import com.kapor.auth.security.CustomUserDetails;
import com.kapor.common.dto.ApiResponse;
import com.kapor.notification.dto.FcmDeviceRegistrationRequest;
import com.kapor.notification.service.FcmDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/devices")
@RequiredArgsConstructor
public class NotificationController {
    private final FcmDeviceService deviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody FcmDeviceRegistrationRequest request,
                                                       Authentication authentication) {
        deviceService.register(userId(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unregister(@RequestParam String token, Authentication authentication) {
        deviceService.unregister(userId(authentication), token);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}

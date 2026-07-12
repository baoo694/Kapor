package com.kapor.user.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.user.dto.UserDto;
import com.kapor.user.dto.UserUpdateRequest;
import com.kapor.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        UserDto userDto = userService.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            Authentication authentication,
            @RequestBody UserUpdateRequest request) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        UserDto updatedUser = userService.updateUser(email, request);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser, "Profile updated successfully"));
    }

    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResponse<UserDto>> completeOnboarding(
            Authentication authentication,
            @RequestBody com.kapor.user.dto.OnboardingRequest request) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        UserDto updatedUser = userService.completeOnboarding(email, request);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser, "Onboarding completed successfully"));
    }
}

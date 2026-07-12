package com.kapor.analytics.controller;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.service.AnalyticsService;
import com.kapor.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            Authentication authentication,
            @RequestParam(defaultValue = "weekly") String period) {
        
        String userId = ((com.kapor.auth.security.CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        DashboardResponse response = analyticsService.getDashboardData(userId, period);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

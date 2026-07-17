package com.kapor.admin.controller;

import com.kapor.admin.dto.AdminDashboardStatsDto;
import com.kapor.admin.service.AdminService;
import com.kapor.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getDashboardStats() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.kapor.user.dto.UserDto>>> getUsers(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getUsers(page, search)));
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<com.kapor.user.dto.UserDto>> updateUserRole(
            @org.springframework.web.bind.annotation.PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody com.kapor.admin.dto.RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateUserRole(id, request.getRole())));
    }

    @org.springframework.web.bind.annotation.PostMapping("/users")
    public ResponseEntity<ApiResponse<com.kapor.user.dto.UserDto>> createUser(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.kapor.admin.dto.CreateUserRequest request) {
        com.kapor.user.dto.UserDto user = adminService.createUser(request);
        return ResponseEntity.ok(ApiResponse.ok(user, "User created successfully"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@org.springframework.web.bind.annotation.PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted successfully"));
    }
}

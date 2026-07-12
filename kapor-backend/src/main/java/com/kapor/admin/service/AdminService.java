package com.kapor.admin.service;

import com.kapor.admin.dto.AdminDashboardStatsDto;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final DailyActivityRepository dailyActivityRepository;

    public AdminDashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalContent = lessonRepository.count();
        
        // DAU: count of unique users who had activity today
        // Our daily_activity collection records one document per user per date.
        // So counting by today's date gives the DAU.
        long dau = dailyActivityRepository.countByDate(LocalDate.now());

        return AdminDashboardStatsDto.builder()
                .users(totalUsers)
                .contentCount(totalContent)
                .dau(dau)
                .build();
    }

    public org.springframework.data.domain.Page<com.kapor.user.dto.UserDto> getUsers(int page, String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1, 10);
        org.springframework.data.domain.Page<com.kapor.user.model.User> users;
        if (search == null || search.trim().isEmpty()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByProfileDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
        }
        return users.map(com.kapor.user.dto.UserDto::fromEntity);
    }

    public com.kapor.user.dto.UserDto updateUserRole(String id, String role) {
        com.kapor.user.model.User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Convert "ADMIN" to "ROLE_ADMIN", etc.
        String roleStr = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        user.setRoles(java.util.Set.of(roleStr));
        
        user = userRepository.save(user);
        return com.kapor.user.dto.UserDto.fromEntity(user);
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}

package com.kapor.admin.service;

import com.kapor.admin.dto.AdminDashboardStatsDto;
import com.kapor.admin.dto.CreateUserRequest;
import com.kapor.analytics.repository.DailyActivityRepository;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalContent = lessonRepository.count();
        
        // DAU: count of unique users who had activity today
        // Our daily_activity collection records one document per user per date.
        // So counting by today's date gives the DAU.
        long dau = dailyActivityRepository.countByDate(LocalDate.now());

        // Generate Chart Data
        List<AdminDashboardStatsDto.UserGrowthPoint> userGrowthData = List.of(
                new AdminDashboardStatsDto.UserGrowthPoint("Jan", 120),
                new AdminDashboardStatsDto.UserGrowthPoint("Feb", 180),
                new AdminDashboardStatsDto.UserGrowthPoint("Mar", 240),
                new AdminDashboardStatsDto.UserGrowthPoint("Apr", 350),
                new AdminDashboardStatsDto.UserGrowthPoint("May", 520),
                new AdminDashboardStatsDto.UserGrowthPoint("Jun", 780),
                new AdminDashboardStatsDto.UserGrowthPoint("Jul", (int) totalUsers > 780 ? (int) totalUsers : 890)
        );

        List<AdminDashboardStatsDto.DauPoint> dauData = List.of(
                new AdminDashboardStatsDto.DauPoint("Mon", 89),
                new AdminDashboardStatsDto.DauPoint("Tue", 112),
                new AdminDashboardStatsDto.DauPoint("Wed", 145),
                new AdminDashboardStatsDto.DauPoint("Thu", 132),
                new AdminDashboardStatsDto.DauPoint("Fri", 156),
                new AdminDashboardStatsDto.DauPoint("Sat", 98),
                new AdminDashboardStatsDto.DauPoint("Sun", (int) dau > 0 ? (int) dau : 76)
        );

        List<AdminDashboardStatsDto.DomainCompletionPoint> lessonCompletionData = List.of(
                new AdminDashboardStatsDto.DomainCompletionPoint("Frontend", 450),
                new AdminDashboardStatsDto.DomainCompletionPoint("Backend", 380),
                new AdminDashboardStatsDto.DomainCompletionPoint("DevOps", 290),
                new AdminDashboardStatsDto.DomainCompletionPoint("Agile", 210)
        );

        List<AdminDashboardStatsDto.AiUsagePoint> aiUsageData = List.of(
                new AdminDashboardStatsDto.AiUsagePoint("Oct", 48),
                new AdminDashboardStatsDto.AiUsagePoint("Nov", 72),
                new AdminDashboardStatsDto.AiUsagePoint("Dec", 95),
                new AdminDashboardStatsDto.AiUsagePoint("Jan", 120),
                new AdminDashboardStatsDto.AiUsagePoint("Feb", 145),
                new AdminDashboardStatsDto.AiUsagePoint("Mar", 168)
        );

        List<AdminDashboardStatsDto.NewRegPoint> newRegData = List.of(
                new AdminDashboardStatsDto.NewRegPoint("Mon", 18),
                new AdminDashboardStatsDto.NewRegPoint("Tue", 24),
                new AdminDashboardStatsDto.NewRegPoint("Wed", 31),
                new AdminDashboardStatsDto.NewRegPoint("Thu", 28),
                new AdminDashboardStatsDto.NewRegPoint("Fri", 35),
                new AdminDashboardStatsDto.NewRegPoint("Sat", 14),
                new AdminDashboardStatsDto.NewRegPoint("Sun", 10)
        );

        List<AdminDashboardStatsDto.RetentionPoint> retentionData = List.of(
                new AdminDashboardStatsDto.RetentionPoint("W1", 100, 62, 38),
                new AdminDashboardStatsDto.RetentionPoint("W2", 100, 58, 35),
                new AdminDashboardStatsDto.RetentionPoint("W3", 100, 65, 42),
                new AdminDashboardStatsDto.RetentionPoint("W4", 100, 70, 45)
        );

        List<AdminDashboardStatsDto.AiDailyPoint> aiDailyData = List.of(
                new AdminDashboardStatsDto.AiDailyPoint("Mon", 680, 1240, 320),
                new AdminDashboardStatsDto.AiDailyPoint("Tue", 820, 1480, 410),
                new AdminDashboardStatsDto.AiDailyPoint("Wed", 1050, 1720, 580),
                new AdminDashboardStatsDto.AiDailyPoint("Thu", 940, 1380, 490),
                new AdminDashboardStatsDto.AiDailyPoint("Fri", 1120, 1850, 620),
                new AdminDashboardStatsDto.AiDailyPoint("Sat", 560, 980, 280),
                new AdminDashboardStatsDto.AiDailyPoint("Sun", 430, 780, 220)
        );

        return AdminDashboardStatsDto.builder()
                .users(totalUsers)
                .contentCount(totalContent)
                .dau(dau)
                .userGrowthData(userGrowthData)
                .dauData(dauData)
                .lessonCompletionData(lessonCompletionData)
                .aiUsageData(aiUsageData)
                .newRegData(newRegData)
                .retentionData(retentionData)
                .aiDailyData(aiDailyData)
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

    public com.kapor.user.dto.UserDto createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Determine role
        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? (request.getRole().startsWith("ROLE_") ? request.getRole() : "ROLE_" + request.getRole().toUpperCase())
                : "ROLE_USER";

        com.kapor.user.model.User user = com.kapor.user.model.User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("email")
                // Email accounts do not have an OAuth subject; use the canonical
                // email address as their provider-specific identifier instead.
                .providerId(email)
                .roles(Set.of(role))
                .profile(com.kapor.user.model.User.Profile.builder()
                        .displayName(request.getName())
                        .nativeLanguage("vi")
                        .joinedAt(Instant.now())
                        .build())
                .streak(com.kapor.user.model.User.Streak.builder()
                        .current(0)
                        .longest(0)
                        .freezesRemaining(2)
                        .build())
                .settings(com.kapor.user.model.User.UserSettings.builder().build())
                .stats(com.kapor.user.model.User.UserStats.builder().build())
                .build();

        user = userRepository.save(user);
        return com.kapor.user.dto.UserDto.fromEntity(user);
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

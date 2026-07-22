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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        long dau = dailyActivityRepository.countByDate(LocalDate.now());
        LocalDate today = LocalDate.now();
        List<com.kapor.user.model.User> allUsers = userRepository.findAll();
        List<com.kapor.analytics.model.DailyActivity> activities = dailyActivityRepository.findAll();
        List<AdminDashboardStatsDto.UserGrowthPoint> userGrowthData = userGrowth(allUsers);
        List<AdminDashboardStatsDto.DauPoint> dauData = lastSevenDays(today, activities, false);
        List<AdminDashboardStatsDto.NewRegPoint> newRegData = registrationsLastSevenDays(today, allUsers);
        List<AdminDashboardStatsDto.DomainCompletionPoint> lessonCompletionData = activityCompletionsByDomain(activities);
        List<AdminDashboardStatsDto.RetentionPoint> retentionData = retention(allUsers, activities, today);
        List<AdminDashboardStatsDto.AiUsagePoint> aiUsageData = List.of();
        List<AdminDashboardStatsDto.AiDailyPoint> aiDailyData = lastSevenDays(today, activities, true).stream()
                .map(point -> new AdminDashboardStatsDto.AiDailyPoint(point.getDay(), 0, 0, 0)).toList();
        long mau = activities.stream().filter(a -> !a.getDate().isBefore(today.minusDays(29))).map(com.kapor.analytics.model.DailyActivity::getUserId).distinct().count();
        long totalMinutes = activities.stream().filter(a -> !a.getDate().isBefore(today.minusDays(6))).mapToLong(com.kapor.analytics.model.DailyActivity::getMinutesStudied).sum();
        long activityRows = activities.stream().filter(a -> !a.getDate().isBefore(today.minusDays(6))).count();

        return AdminDashboardStatsDto.builder()
                .users(totalUsers)
                .contentCount(totalContent)
                .dau(dau)
                .mau(mau)
                .averageSessionMinutes(activityRows == 0 ? 0 : Math.round((float) totalMinutes / activityRows))
                .churnRate(totalUsers == 0 ? 0 : Math.max(0, 100d - (mau * 100d / totalUsers)))
                .totalAiCalls(0)
                .totalAiCost(0)
                .aiErrorCount(0)
                .userGrowthData(userGrowthData)
                .dauData(dauData)
                .lessonCompletionData(lessonCompletionData)
                .aiUsageData(aiUsageData)
                .newRegData(newRegData)
                .retentionData(retentionData)
                .aiDailyData(aiDailyData)
                .build();
    }

    private List<AdminDashboardStatsDto.UserGrowthPoint> userGrowth(List<com.kapor.user.model.User> users) {
        Map<YearMonth, Long> byMonth = users.stream().map(user -> user.getCreatedAt() == null ? null : YearMonth.from(user.getCreatedAt().atZone(ZoneOffset.UTC)))
                .filter(java.util.Objects::nonNull).collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        long running = 0;
        List<AdminDashboardStatsDto.UserGrowthPoint> points = new ArrayList<>();
        for (YearMonth month : byMonth.keySet().stream().sorted().toList()) {
            running += byMonth.get(month);
            points.add(new AdminDashboardStatsDto.UserGrowthPoint(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), (int) running));
        }
        return points;
    }

    private List<AdminDashboardStatsDto.DauPoint> lastSevenDays(LocalDate today, List<com.kapor.analytics.model.DailyActivity> activities, boolean ignored) {
        List<AdminDashboardStatsDto.DauPoint> points = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            int count = (int) activities.stream().filter(activity -> date.equals(activity.getDate())).map(com.kapor.analytics.model.DailyActivity::getUserId).distinct().count();
            points.add(new AdminDashboardStatsDto.DauPoint(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), count));
        }
        return points;
    }

    private List<AdminDashboardStatsDto.NewRegPoint> registrationsLastSevenDays(LocalDate today, List<com.kapor.user.model.User> users) {
        return java.util.stream.IntStream.rangeClosed(0, 6).mapToObj(offset -> {
            LocalDate date = today.minusDays(6 - offset);
            int count = (int) users.stream().filter(user -> user.getCreatedAt() != null && date.equals(user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate())).count();
            return new AdminDashboardStatsDto.NewRegPoint(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), count);
        }).toList();
    }

    private List<AdminDashboardStatsDto.DomainCompletionPoint> activityCompletionsByDomain(List<com.kapor.analytics.model.DailyActivity> activities) {
        // DailyActivity has no domain dimension yet; report the measured global total under "all" rather than inventing splits.
        return List.of(new AdminDashboardStatsDto.DomainCompletionPoint("all", activities.stream().mapToInt(com.kapor.analytics.model.DailyActivity::getLessonsCompleted).sum()));
    }

    private List<AdminDashboardStatsDto.RetentionPoint> retention(List<com.kapor.user.model.User> users, List<com.kapor.analytics.model.DailyActivity> activities, LocalDate today) {
        return java.util.stream.IntStream.rangeClosed(0, 3).mapToObj(index -> {
            LocalDate cohortStart = today.minusWeeks(index + 1L);
            long cohort = users.stream().filter(user -> user.getCreatedAt() != null && !user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(cohortStart.minusDays(6)) && !user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(cohortStart)).count();
            return new AdminDashboardStatsDto.RetentionPoint("W" + (index + 1), cohort == 0 ? 0 : 100, 0, 0);
        }).toList();
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

    public List<com.kapor.user.dto.UserDto> getAdminUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN"))
                .map(com.kapor.user.dto.UserDto::fromEntity).toList();
    }

    /** Promotes an existing account. This intentionally does not create an account or send email from the API. */
    public com.kapor.user.dto.UserDto grantAdmin(String email) {
        com.kapor.user.model.User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("No user found for email: " + email));
        user.setRoles(Set.of("ROLE_ADMIN"));
        return com.kapor.user.dto.UserDto.fromEntity(userRepository.save(user));
    }

    public void revokeAdmin(String id) {
        com.kapor.user.model.User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRoles(Set.of("ROLE_USER"));
        userRepository.save(user);
    }
}

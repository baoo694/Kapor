package com.kapor.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash; // null if OAuth

    private String provider;     // google | email
    private String providerId;   // Google UID

    private Profile profile;
    private Streak streak;
    private UserSettings settings;
    private UserStats stats;

    private String refreshToken;
    private Instant refreshTokenExpiry;

    private Set<String> roles; // ROLE_USER, ROLE_ADMIN

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String displayName;
        private String avatarUrl;
        @Builder.Default
        private String nativeLanguage = "vi";
        @Builder.Default
        private String koreanLevel = "beginner"; // beginner | intermediate | advanced
        private List<String> learningGoals;
        private Instant joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Streak {
        @Builder.Default
        private int current = 0;
        @Builder.Default
        private int longest = 0;
        private LocalDate lastActiveDate;
        @Builder.Default
        private int freezesRemaining = 2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSettings {
        @Builder.Default
        private double ttsSpeed = 1.0;
        @Builder.Default
        private int dailyGoalMinutes = 15;
        @Builder.Default
        private int dailyGoalCards = 20;
        @Builder.Default
        private String theme = "dark";     // dark | light | system
        @Builder.Default
        private String locale = "vi";      // vi | en
        @Builder.Default
        private boolean notificationsEnabled = true;
        private String reminderTime;       // "09:00"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        @Builder.Default
        private long totalStudyMinutes = 0;
        @Builder.Default
        private long totalCardsReviewed = 0;
        @Builder.Default
        private long totalRoleplaySessions = 0;
        @Builder.Default
        private long totalVideosWatched = 0;
    }
}

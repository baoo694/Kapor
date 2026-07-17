package com.kapor.user.dto;

import com.kapor.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String nativeLanguage;
    private List<String> learningGoals;
    private boolean hasCompletedOnboarding;
    private User.Streak streak;
    private User.UserSettings settings;
    private User.UserStats stats;
    private Set<String> roles;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getProfile() != null ? user.getProfile().getDisplayName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .nativeLanguage(user.getProfile() != null ? user.getProfile().getNativeLanguage() : null)
                .learningGoals(user.getProfile() != null ? user.getProfile().getLearningGoals() : null)
                .hasCompletedOnboarding(user.getProfile() != null && user.getProfile().getLearningGoals() != null && !user.getProfile().getLearningGoals().isEmpty())
                .streak(user.getStreak())
                .settings(user.getSettings())
                .stats(user.getStats())
                .roles(user.getRoles())
                .build();
    }
}

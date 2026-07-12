package com.kapor.user.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.user.dto.UserDto;
import com.kapor.user.dto.UserUpdateRequest;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return UserDto.fromEntity(user);
    }

    public UserDto updateUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getDisplayName() != null) user.getProfile().setDisplayName(request.getDisplayName());
        if (request.getAvatarUrl() != null) user.getProfile().setAvatarUrl(request.getAvatarUrl());
        if (request.getNativeLanguage() != null) user.getProfile().setNativeLanguage(request.getNativeLanguage());
        if (request.getKoreanLevel() != null) user.getProfile().setKoreanLevel(request.getKoreanLevel());
        
        if (request.getSettings() != null) {
            user.setSettings(request.getSettings());
        }

        userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    public UserDto completeOnboarding(String email, com.kapor.user.dto.OnboardingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getProfile() == null) user.setProfile(new User.Profile());
        if (request.getLearningGoals() != null) {
            user.getProfile().setLearningGoals(request.getLearningGoals());
        }
        if (request.getKoreanLevel() != null) {
            user.getProfile().setKoreanLevel(request.getKoreanLevel());
        }

        if (user.getSettings() == null) user.setSettings(new User.UserSettings());
        if (request.getDailyGoalMinutes() != null) {
            user.getSettings().setDailyGoalMinutes(request.getDailyGoalMinutes());
        }

        userRepository.save(user);
        return UserDto.fromEntity(user);
    }
}

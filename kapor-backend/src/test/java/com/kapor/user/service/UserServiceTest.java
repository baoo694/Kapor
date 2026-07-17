package com.kapor.user.service;

import com.kapor.TestDataFactory;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.user.dto.UserDto;
import com.kapor.user.dto.UserUpdateRequest;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * Tests user retrieval and profile update logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser();
        testUser.setId("user_123");
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("should return UserDto for existing user")
        void shouldReturnUserDtoForExistingUser() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserDto result = userService.getCurrentUser("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("user_123");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getDisplayName()).isEqualTo("Test User");
            assertThat(result.getNativeLanguage()).isEqualTo("vi");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void shouldThrowForNonExistentUser() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUser("nonexistent@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should update display name only")
        void shouldUpdateDisplayNameOnly() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setDisplayName("New Name");

            UserDto result = userService.updateUser("test@example.com", request);

            assertThat(result.getDisplayName()).isEqualTo("New Name");
            // Other fields should remain unchanged
            assertThat(result.getNativeLanguage()).isEqualTo("vi");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should update user settings")
        void shouldUpdateUserSettings() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User.UserSettings newSettings = User.UserSettings.builder()
                    .theme("light")
                    .locale("en")
                    .ttsSpeed(1.5)
                    .dailyGoalMinutes(30)
                    .dailyGoalCards(50)
                    .notificationsEnabled(false)
                    .build();

            UserUpdateRequest request = new UserUpdateRequest();
            request.setSettings(newSettings);

            userService.updateUser("test@example.com", request);

            assertThat(testUser.getSettings().getTheme()).isEqualTo("light");
            assertThat(testUser.getSettings().getLocale()).isEqualTo("en");
            assertThat(testUser.getSettings().getTtsSpeed()).isEqualTo(1.5);
        }

        @Test
        @DisplayName("should not modify null fields in request")
        void shouldNotModifyNullFields() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserUpdateRequest request = new UserUpdateRequest();
            // All fields are null

            userService.updateUser("test@example.com", request);

            // Original values should be unchanged
            assertThat(testUser.getProfile().getDisplayName()).isEqualTo("Test User");
            assertThat(testUser.getProfile().getNativeLanguage()).isEqualTo("vi");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void shouldThrowForNonExistentUser() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            UserUpdateRequest request = new UserUpdateRequest();
            request.setDisplayName("Ghost");

            assertThatThrownBy(() -> userService.updateUser("ghost@example.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

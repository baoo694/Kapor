package com.kapor.user.dto;

import com.kapor.TestDataFactory;
import com.kapor.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserDto} mapping.
 * Tests the fromEntity static factory method for correct field mapping and null safety.
 */
@DisplayName("UserDto Mapping Unit Tests")
class UserDtoTest {

    @Nested
    @DisplayName("fromEntity")
    class FromEntity {

        @Test
        @DisplayName("should correctly map all fields from User entity")
        void shouldCorrectlyMapAllFields() {
            User user = TestDataFactory.createTestUser();
            user.setId("user_123");

            UserDto dto = UserDto.fromEntity(user);

            assertThat(dto.getId()).isEqualTo("user_123");
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getDisplayName()).isEqualTo("Test User");
            assertThat(dto.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(dto.getNativeLanguage()).isEqualTo("vi");
            assertThat(dto.getStreak()).isNotNull();
            assertThat(dto.getSettings()).isNotNull();
            assertThat(dto.getRoles()).contains("ROLE_USER");
        }

        @Test
        @DisplayName("should return null for null user")
        void shouldReturnNullForNullUser() {
            UserDto dto = UserDto.fromEntity(null);

            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("should handle user with null profile gracefully")
        void shouldHandleNullProfile() {
            User user = User.builder()
                    .id("user_no_profile")
                    .email("noprofile@test.com")
                    .build();

            UserDto dto = UserDto.fromEntity(user);

            assertThat(dto).isNotNull();
            assertThat(dto.getEmail()).isEqualTo("noprofile@test.com");
            assertThat(dto.getDisplayName()).isNull();
            assertThat(dto.getAvatarUrl()).isNull();
        }
    }
}

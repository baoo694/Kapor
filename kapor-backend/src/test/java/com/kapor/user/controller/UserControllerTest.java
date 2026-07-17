package com.kapor.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.TestDataFactory;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.user.dto.UserUpdateRequest;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link UserController}.
 * Tests GET /api/users/me and PUT /api/users/me endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Controller Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User savedUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = TestDataFactory.createTestUser();
        savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        accessToken = jwtService.generateToken(userDetails);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user profile")
        void shouldReturnCurrentUserProfile() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(savedUser.getId()))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.displayName").value("Test User"))
                    .andExpect(jsonPath("$.data.nativeLanguage").value("vi"));
        }

        @Test
        @DisplayName("should return streak data in profile")
        void shouldReturnStreakData() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.streak").isNotEmpty())
                    .andExpect(jsonPath("$.data.streak.current").value(0))
                    .andExpect(jsonPath("$.data.streak.longest").value(0));
        }

        @Test
        @DisplayName("should return settings in profile")
        void shouldReturnSettingsData() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.settings.theme").value("dark"))
                    .andExpect(jsonPath("$.data.settings.locale").value("vi"))
                    .andExpect(jsonPath("$.data.settings.dailyGoalMinutes").value(15))
                    .andExpect(jsonPath("$.data.settings.dailyGoalCards").value(20));
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 with invalid token")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer invalid-jwt-token"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    class UpdateUser {

        @Test
        @DisplayName("should update display name")
        void shouldUpdateDisplayName() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setDisplayName("Updated Name");

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.displayName").value("Updated Name"));

            // Verify persistence
            User updated = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(updated.getProfile().getDisplayName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("should update native language")
        void shouldUpdateNativeLanguage() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setNativeLanguage("en");

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nativeLanguage").value("en"));
        }

        @Test
        @DisplayName("should update user settings")
        void shouldUpdateUserSettings() throws Exception {
            User.UserSettings newSettings = User.UserSettings.builder()
                    .theme("light")
                    .locale("en")
                    .ttsSpeed(1.5)
                    .dailyGoalMinutes(30)
                    .dailyGoalCards(40)
                    .notificationsEnabled(false)
                    .reminderTime("08:00")
                    .build();

            UserUpdateRequest request = new UserUpdateRequest();
            request.setSettings(newSettings);

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.settings.theme").value("light"))
                    .andExpect(jsonPath("$.data.settings.locale").value("en"))
                    .andExpect(jsonPath("$.data.settings.dailyGoalMinutes").value(30));
        }

        @Test
        @DisplayName("should not modify unset fields")
        void shouldNotModifyUnsetFields() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setDisplayName("Only Name Changed");
            // Other fields are null — should not be touched

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.displayName").value("Only Name Changed"))
                    .andExpect(jsonPath("$.data.nativeLanguage").value("vi"));  // unchanged
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setDisplayName("Unauthorized");

            mockMvc.perform(put("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}

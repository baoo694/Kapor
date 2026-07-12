package com.kapor.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kapor.auth.dto.GoogleLoginRequest;
import com.kapor.auth.service.GoogleAuthService;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 * Tests Google OAuth login, new user registration, and returning user login flows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private GoogleIdToken.Payload createMockPayload(String email, String googleId, String name) {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.getSubject()).thenReturn(googleId);
        when(payload.get("name")).thenReturn(name);
        when(payload.get("picture")).thenReturn("https://example.com/avatar.jpg");
        return payload;
    }

    @Nested
    @DisplayName("POST /api/auth/google")
    class GoogleLogin {

        @Test
        @DisplayName("should register new user on first Google login")
        void shouldRegisterNewUserOnFirstLogin() throws Exception {
            GoogleIdToken.Payload payload = createMockPayload("new@example.com", "google_new_123", "New User");
            when(googleAuthService.verifyToken(anyString())).thenReturn(payload);

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("valid-google-id-token");

            mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.newUser").value(true))
                    .andExpect(jsonPath("$.data.user.email").value("new@example.com"))
                    .andExpect(jsonPath("$.data.user.displayName").value("New User"));

            // Verify user was persisted in MongoDB
            Optional<User> savedUser = userRepository.findByEmail("new@example.com");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getProvider()).isEqualTo("google");
            assertThat(savedUser.get().getProviderId()).isEqualTo("google_new_123");
        }

        @Test
        @DisplayName("should login existing user without creating duplicate")
        void shouldLoginExistingUser() throws Exception {
            // Pre-create user
            GoogleIdToken.Payload payload = createMockPayload("existing@example.com", "google_existing_123", "Existing User");
            when(googleAuthService.verifyToken(anyString())).thenReturn(payload);

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("valid-google-id-token");

            // First login → creates user
            mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newUser").value(true));

            // Second login → existing user
            mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newUser").value(false))
                    .andExpect(jsonPath("$.data.user.email").value("existing@example.com"));

            // Should still be only one user
            long userCount = userRepository.count();
            assertThat(userCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should return error for invalid Google ID token")
        void shouldReturnErrorForInvalidToken() throws Exception {
            when(googleAuthService.verifyToken(anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid Google ID token."));

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("invalid-token");

            mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return validation error for empty idToken")
        void shouldReturnValidationErrorForEmptyToken() throws Exception {
            GoogleLoginRequest request = new GoogleLoginRequest();
            // idToken is not set, should be blank

            mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return tokens with correct format")
        void shouldReturnTokensWithCorrectFormat() throws Exception {
            GoogleIdToken.Payload payload = createMockPayload("token-test@example.com", "google_token_123", "Token User");
            when(googleAuthService.verifyToken(anyString())).thenReturn(payload);

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("valid-token");

            MvcResult result = mockMvc.perform(post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("accessToken", "refreshToken");
        }
    }
}

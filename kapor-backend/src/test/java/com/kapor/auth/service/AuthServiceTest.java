package com.kapor.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kapor.auth.dto.AuthResponse;
import com.kapor.auth.dto.GoogleLoginRequest;
import com.kapor.auth.security.JwtService;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * Tests the Google OAuth login flow: token verification, user creation/retrieval, and JWT generation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private GoogleIdToken.Payload mockPayload;

    @BeforeEach
    void setUp() {
        mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn("user@example.com");
        when(mockPayload.getSubject()).thenReturn("google_uid_456");
        when(mockPayload.get("name")).thenReturn("Test User");
        when(mockPayload.get("picture")).thenReturn("https://example.com/pic.jpg");
    }

    @Nested
    @DisplayName("New User Registration")
    class NewUserRegistration {

        @Test
        @DisplayName("should create new user on first login")
        void shouldCreateNewUserOnFirstLogin() {
            when(googleAuthService.verifyToken(anyString())).thenReturn(mockPayload);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh_token");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId("new_user_id");
                return u;
            });

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("valid_token");

            AuthResponse response = authService.loginWithGoogle(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.isNewUser()).isTrue();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should save new user with correct provider info")
        void shouldSaveNewUserWithCorrectProviderInfo() {
            when(googleAuthService.verifyToken(anyString())).thenReturn(mockPayload);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
            when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("token");

            authService.loginWithGoogle(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getProvider()).isEqualTo("google");
            assertThat(savedUser.getProviderId()).isEqualTo("google_uid_456");
            assertThat(savedUser.getProfile().getDisplayName()).isEqualTo("Test User");
            assertThat(savedUser.getRoles()).contains("ROLE_USER");
        }

        @Test
        @DisplayName("should initialize default streak and settings for new user")
        void shouldInitializeDefaultsForNewUser() {
            when(googleAuthService.verifyToken(anyString())).thenReturn(mockPayload);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
            when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("token");

            authService.loginWithGoogle(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getStreak()).isNotNull();
            assertThat(savedUser.getSettings()).isNotNull();
            assertThat(savedUser.getStats()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Existing User Login")
    class ExistingUserLogin {

        @Test
        @DisplayName("should return existing user without creating duplicate")
        void shouldReturnExistingUser() {
            User existingUser = User.builder()
                    .id("existing_id")
                    .email("user@example.com")
                    .provider("google")
                    .providerId("google_uid_456")
                    .roles(java.util.Collections.singleton("ROLE_USER"))
                    .profile(User.Profile.builder()
                            .displayName("Existing User")
                            .build())
                    .streak(new User.Streak())
                    .settings(new User.UserSettings())
                    .stats(new User.UserStats())
                    .build();

            when(googleAuthService.verifyToken(anyString())).thenReturn(mockPayload);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh_token");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("valid_token");

            AuthResponse response = authService.loginWithGoogle(request);

            assertThat(response.isNewUser()).isFalse();
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Token Verification Failure")
    class TokenVerificationFailure {

        @Test
        @DisplayName("should throw when Google token verification fails")
        void shouldThrowWhenTokenVerificationFails() {
            when(googleAuthService.verifyToken(anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid token"));

            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("bad_token");

            assertThatThrownBy(() -> authService.loginWithGoogle(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid token");
        }
    }
}

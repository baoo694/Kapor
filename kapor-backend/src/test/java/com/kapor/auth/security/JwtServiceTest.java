package com.kapor.auth.security;

import com.kapor.TestDataFactory;
import com.kapor.user.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 * Tests JWT token generation, validation, extraction, and expiration.
 */
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    // Valid Base64-encoded 256-bit key for HS256
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1rYXBvci1iYWNrZW5kLXRlc3RzLW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=";
    private static final long ACCESS_TOKEN_EXPIRATION = 900_000L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 2_592_000_000L; // 30 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_TOKEN_EXPIRATION);

        User user = TestDataFactory.createTestUser();
        userDetails = new CustomUserDetails(user);
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("should generate a valid access token")
        void shouldGenerateAccessToken() {
            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotNull().isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("should generate a valid refresh token")
        void shouldGenerateRefreshToken() {
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertThat(refreshToken).isNotNull().isNotBlank();
            assertThat(refreshToken.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("access token and refresh token should be different")
        void accessAndRefreshTokensShouldBeDifferent() {
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertThat(accessToken).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = jwtService.generateToken(userDetails);

            User anotherUser = TestDataFactory.createTestUser("other@example.com", "google_other_uid");
            UserDetails otherDetails = new CustomUserDetails(anotherUser);
            String token2 = jwtService.generateToken(otherDetails);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Username Extraction")
    class UsernameExtraction {

        @Test
        @DisplayName("should extract username (email) from access token")
        void shouldExtractUsernameFromAccessToken() {
            String token = jwtService.generateToken(userDetails);

            String extractedUsername = jwtService.extractUsername(token);

            assertThat(extractedUsername).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should extract username from refresh token")
        void shouldExtractUsernameFromRefreshToken() {
            String token = jwtService.generateRefreshToken(userDetails);

            String extractedUsername = jwtService.extractUsername(token);

            assertThat(extractedUsername).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should validate a correct token as valid")
        void shouldValidateCorrectToken() {
            String token = jwtService.generateToken(userDetails);

            boolean isValid = jwtService.isTokenValid(token, userDetails);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should invalidate token for different user")
        void shouldInvalidateTokenForDifferentUser() {
            String token = jwtService.generateToken(userDetails);

            User anotherUser = TestDataFactory.createTestUser("other@example.com", "google_other_uid");
            UserDetails otherDetails = new CustomUserDetails(anotherUser);

            boolean isValid = jwtService.isTokenValid(token, otherDetails);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should reject malformed JWT token")
        void shouldRejectMalformedToken() {
            assertThatThrownBy(() -> jwtService.extractUsername("not-a-valid-jwt"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Set expiration to 0 to create an immediately expired token
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 0L);
            String token = jwtService.generateToken(userDetails);

            // Reset expiration for validation
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_TOKEN_EXPIRATION);

            assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}

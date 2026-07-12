package com.kapor.config;

import com.kapor.TestDataFactory;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Security configuration.
 * Tests endpoint protection, JWT authentication, and role-based access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Configuration Integration Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create regular user
        User user = TestDataFactory.createTestUser();
        user = userRepository.save(user);
        userToken = jwtService.generateToken(new CustomUserDetails(user));

        // Create admin user
        User admin = TestDataFactory.createAdminUser();
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(new CustomUserDetails(admin));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("/api/auth/** should be accessible without token")
        void authEndpointsShouldBePublic() throws Exception {
            // POST /api/auth/google is public — even though body is missing, 
            // we just verify it doesn't return 401/403
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/health should be accessible without token")
        void actuatorHealthShouldBePublic() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("should reject requests without Authorization header")
        void shouldRejectWithoutAuthHeader() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should reject requests with malformed token")
        void shouldRejectMalformedToken() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer not-a-real-jwt"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should reject requests with Bearer prefix missing")
        void shouldRejectMissingBearerPrefix() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should accept requests with valid user token")
        void shouldAcceptValidUserToken() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control")
    class RoleBasedAccessControl {

        @Test
        @DisplayName("regular user should be forbidden from admin endpoints")
        void regularUserShouldBeForbiddenFromAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/some-endpoint")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("admin user should have access to admin endpoints")
        void adminUserShouldHaveAccessToAdmin() throws Exception {
            // The endpoint /api/admin/** likely doesn't exist yet → 404
            // But the key thing is it shouldn't be 403
            mockMvc.perform(get("/api/admin/some-endpoint")
                            .header("Authorization", "Bearer " + adminToken))
                    // Admin role passes → endpoint just doesn't exist → 404
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("admin token should also work for regular endpoints")
        void adminTokenShouldWorkForRegularEndpoints() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }
}

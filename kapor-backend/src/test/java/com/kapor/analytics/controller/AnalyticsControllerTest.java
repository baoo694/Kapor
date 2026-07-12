package com.kapor.analytics.controller;

import com.kapor.TestDataFactory;
import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.repository.DailyActivityRepository;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AnalyticsController}.
 * Tests dashboard endpoint with streak, progress metrics, and recommendation data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Analytics Controller Integration Tests")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyActivityRepository dailyActivityRepository;

    @Autowired
    private JwtService jwtService;

    private User savedUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        dailyActivityRepository.deleteAll();
        userRepository.deleteAll();

        User user = TestDataFactory.createTestUser();
        user.getStreak().setCurrent(5);
        user.getStreak().setLongest(15);
        user.getStreak().setLastActiveDate(LocalDate.now());
        savedUser = userRepository.save(user);

        accessToken = jwtService.generateToken(new CustomUserDetails(savedUser));
    }

    @AfterEach
    void cleanUp() {
        dailyActivityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/analytics/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard with streak data")
        void shouldReturnDashboardWithStreakData() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("period", "weekly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.streak.currentStreak").value(5))
                    .andExpect(jsonPath("$.data.streak.longestStreak").value(15))
                    .andExpect(jsonPath("$.data.streak.activeToday").value(true));
        }

        @Test
        @DisplayName("should return progress metrics with weekly period")
        void shouldReturnWeeklyProgressMetrics() throws Exception {
            // Seed daily activities for the past week
            for (int i = 0; i < 5; i++) {
                DailyActivity activity = TestDataFactory.createDailyActivity(savedUser.getId(), LocalDate.now().minusDays(i));
                dailyActivityRepository.save(activity);
            }

            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("period", "weekly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.progress").isNotEmpty())
                    .andExpect(jsonPath("$.data.progress.period").value("weekly"))
                    .andExpect(jsonPath("$.data.progress.speaking").isNumber())
                    .andExpect(jsonPath("$.data.progress.vocabulary").isNumber())
                    .andExpect(jsonPath("$.data.progress.listening").isNumber())
                    .andExpect(jsonPath("$.data.progress.roleplayScore").isNumber());
        }

        @Test
        @DisplayName("should return zero progress when no activities exist")
        void shouldReturnZeroProgressWhenNoActivities() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("period", "weekly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.progress.speaking").value(0))
                    .andExpect(jsonPath("$.data.progress.vocabulary").value(0))
                    .andExpect(jsonPath("$.data.progress.listening").value(0))
                    .andExpect(jsonPath("$.data.progress.roleplayScore").value(0));
        }

        @Test
        @DisplayName("should return smart recommendation")
        void shouldReturnSmartRecommendation() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendation").isNotEmpty())
                    .andExpect(jsonPath("$.data.recommendation.type").isString())
                    .andExpect(jsonPath("$.data.recommendation.title").isString())
                    .andExpect(jsonPath("$.data.recommendation.targetScreen").isString());
        }

        @Test
        @DisplayName("should default to weekly period when not specified")
        void shouldDefaultToWeeklyPeriod() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.progress.period").value("weekly"));
        }

        @Test
        @DisplayName("should support monthly period parameter")
        void shouldSupportMonthlyPeriod() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("period", "monthly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.progress.period").value("monthly"));
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/analytics/dashboard"))
                    .andExpect(status().isForbidden());
        }
    }
}

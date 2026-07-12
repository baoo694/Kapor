package com.kapor.analytics.service;

import com.kapor.TestDataFactory;
import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RecommendationService}.
 * Tests the static/default recommendation generation logic.
 */
@DisplayName("Recommendation Service Unit Tests")
class RecommendationServiceTest {

    private final RecommendationService recommendationService = new RecommendationService();

    @Test
    @DisplayName("should return a valid recommendation card")
    void shouldReturnValidRecommendation() {
        User user = TestDataFactory.createTestUser();

        DashboardResponse.RecommendationCard card = recommendationService.generateRecommendation(user);

        assertThat(card).isNotNull();
        assertThat(card.getType()).isNotBlank();
        assertThat(card.getTitle()).isNotBlank();
        assertThat(card.getSubtitle()).isNotBlank();
        assertThat(card.getTargetScreen()).isNotBlank();
        assertThat(card.getIcon()).isNotBlank();
    }

    @Test
    @DisplayName("should return review_due type recommendation")
    void shouldReturnReviewDueType() {
        User user = TestDataFactory.createTestUser();

        DashboardResponse.RecommendationCard card = recommendationService.generateRecommendation(user);

        assertThat(card.getType()).isEqualTo("review_due");
    }

    @Test
    @DisplayName("should return valid navigation target screen")
    void shouldReturnValidTargetScreen() {
        User user = TestDataFactory.createTestUser();

        DashboardResponse.RecommendationCard card = recommendationService.generateRecommendation(user);

        assertThat(card.getTargetScreen()).startsWith("/");
    }
}

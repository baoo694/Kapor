package com.kapor.analytics.service;

import com.kapor.TestDataFactory;
import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.membyte.model.MembyteFlashcard;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecommendationService}.
 * Tests the static/default recommendation generation logic.
 */
@DisplayName("Recommendation Service Unit Tests")
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private MembyteFlashcardRepository flashcardRepository;

    @Mock
    private LearningProgressRepository learningProgressRepository;

    @Mock
    private TopicRepository topicRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                flashcardRepository,
                learningProgressRepository,
                topicRepository);
    }

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
    @DisplayName("should recommend due MemByte cards before other activities")
    void shouldReturnReviewDueType() {
        User user = TestDataFactory.createTestUser();
        when(flashcardRepository.findByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(MembyteFlashcard.builder()
                        .userId(user.getId())
                        .isNew(false)
                        .dueAt(Instant.now().minusSeconds(60))
                        .build()));

        DashboardResponse.RecommendationCard card = recommendationService.generateRecommendation(user);

        assertThat(card.getType()).isEqualTo("review_due");
        assertThat(card.getTargetScreen()).isEqualTo("/membyte-review/all");
    }

    @Test
    @DisplayName("should return valid navigation target screen")
    void shouldReturnValidTargetScreen() {
        User user = TestDataFactory.createTestUser();

        DashboardResponse.RecommendationCard card = recommendationService.generateRecommendation(user);

        assertThat(card.getTargetScreen()).startsWith("/");
    }
}

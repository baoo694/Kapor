package com.kapor;

import com.kapor.analytics.model.DailyActivity;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.user.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Factory class providing test data builders for all domain models.
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    // ==================== User ====================

    public static User createTestUser() {
        return createTestUser("test@example.com", "google_uid_123");
    }

    public static User createTestUser(String email, String googleId) {
        return User.builder()
                .email(email)
                .provider("google")
                .providerId(googleId)
                .roles(Collections.singleton("ROLE_USER"))
                .profile(User.Profile.builder()
                        .displayName("Test User")
                        .avatarUrl("https://example.com/avatar.jpg")
                        .nativeLanguage("vi")
                        .joinedAt(Instant.now())
                        .build())
                .streak(new User.Streak())
                .settings(new User.UserSettings())
                .stats(new User.UserStats())
                .build();
    }

    public static User createAdminUser() {
        return User.builder()
                .email("admin@kapor.app")
                .provider("google")
                .providerId("google_admin_uid")
                .roles(new HashSet<>(Set.of("ROLE_USER", "ROLE_ADMIN")))
                .profile(User.Profile.builder()
                        .displayName("Admin User")
                        .avatarUrl("https://example.com/admin-avatar.jpg")
                        .nativeLanguage("en")
                        .joinedAt(Instant.now())
                        .build())
                .streak(new User.Streak())
                .settings(new User.UserSettings())
                .stats(new User.UserStats())
                .build();
    }

    public static User createUserWithStreak(int currentStreak, int longestStreak, LocalDate lastActive) {
        User user = createTestUser();
        User.Streak streak = user.getStreak();
        streak.setCurrent(currentStreak);
        streak.setLongest(longestStreak);
        streak.setLastActiveDate(lastActive);
        return user;
    }

    // ==================== Topic ====================

    public static Topic createTestTopic(String domain) {
        return Topic.builder()
                .id(UUID.randomUUID().toString())
                .domain(domain)
                .title("CSS Grid & Flexbox 용어")
                .titleVi("Thuật ngữ CSS Grid & Flexbox")
                .description("Master CSS layout terminology in Korean")
                .order(1)
                .prerequisiteTopicIds(new ArrayList<>())
                .tags(List.of("CSS", "layout", domain))
                .isActive(true)
                .build();
    }

    public static Topic createTestTopicWithPrerequisites(String domain, List<String> prereqIds) {
        Topic topic = createTestTopic(domain);
        topic.setPrerequisiteTopicIds(prereqIds);
        topic.setOrder(2);
        return topic;
    }

    // ==================== Lesson ====================

    public static Lesson createTestLesson(String topicId, int order) {
        return Lesson.builder()
                .topicId(topicId)
                .title("Flexbox 방향 속성 Lesson " + order)
                .titleVi("Thuộc tính hướng Flexbox Lesson " + order)
                .content("Flexbox에서 방향을 설정하는 속성들을 배워봅시다...")
                .contentVi("Hãy học các thuộc tính thiết lập hướng trong Flexbox...")
                .order(order)
                .vocabulary(List.of(
                        Lesson.VocabularyItem.builder()
                                .id(UUID.randomUUID().toString())
                                .korean("방향")
                                .pronunciation("banghyang")
                                .vietnamese("Hướng")
                                .english("Direction")
                                .context("CSS flex-direction property")
                                .codeSnippet(".container { flex-direction: row; }")
                                .build(),
                        Lesson.VocabularyItem.builder()
                                .id(UUID.randomUUID().toString())
                                .korean("정렬")
                                .pronunciation("jeongnyeol")
                                .vietnamese("Căn chỉnh")
                                .english("Alignment")
                                .context("CSS alignment (justify-content, align-items)")
                                .codeSnippet(".container { justify-content: center; }")
                                .build()
                ))
                .exercises(List.of(
                        Lesson.Exercise.builder()
                                .id(UUID.randomUUID().toString())
                                .type("multiple_choice")
                                .question("'정렬'은 무슨 뜻입니까?")
                                .questionVi("'정렬' có nghĩa là gì?")
                                .options(List.of("Alignment", "Direction", "Spacing", "Wrapping"))
                                .correctAnswer("Alignment")
                                .build()
                ))
                .build();
    }

    // ==================== DailyActivity ====================

    public static DailyActivity createDailyActivity(String userId, LocalDate date) {
        return DailyActivity.builder()
                .userId(userId)
                .date(date)
                .cardsReviewed(20)
                .minutesStudied(15)
                .roleplaySessions(1)
                .lessonsCompleted(2)
                .videosWatched(1)
                .metrics(DailyActivity.DailyMetrics.builder()
                        .speakingScore(75)
                        .vocabularyScore(85)
                        .listeningScore(60)
                        .roleplayScore(78)
                        .build())
                .build();
    }

    // ==================== LearningProgress ====================

    public static LearningProgress createLearningProgress(String userId, String topicId, String domain) {
        return LearningProgress.builder()
                .userId(userId)
                .topicId(topicId)
                .domain(domain)
                .completedLessons(2)
                .totalLessons(5)
                .completionPercent(40.0)
                .isUnlocked(true)
                .build();
    }

    public static LearningProgress createCompletedProgress(String userId, String topicId, String domain) {
        return LearningProgress.builder()
                .userId(userId)
                .topicId(topicId)
                .domain(domain)
                .completedLessons(5)
                .totalLessons(5)
                .completionPercent(100.0)
                .isUnlocked(true)
                .build();
    }
}

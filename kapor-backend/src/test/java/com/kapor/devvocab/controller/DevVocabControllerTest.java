package com.kapor.devvocab.controller;

import com.kapor.TestDataFactory;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link TopicController} and {@link LessonController}.
 * Tests topic listing by domain, skill tree with prerequisites, and lesson CRUD.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DevVocab Controller Integration Tests")
class DevVocabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LearningProgressRepository progressRepository;

    @Autowired
    private JwtService jwtService;

    private User savedUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();

        User user = TestDataFactory.createTestUser();
        savedUser = userRepository.save(user);
        accessToken = jwtService.generateToken(new CustomUserDetails(savedUser));
    }

    @AfterEach
    void cleanUp() {
        progressRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ===========================================
    //  Topic Controller Tests
    // ===========================================

    @Nested
    @DisplayName("GET /api/topics")
    class GetTopicsByDomain {

        @Test
        @DisplayName("should return topics filtered by domain")
        void shouldReturnTopicsFilteredByDomain() throws Exception {
            // Seed frontend and backend topics
            Topic frontend1 = TestDataFactory.createTestTopic("frontend");
            frontend1.setTitle("HTML Basics 기본");
            frontend1.setOrder(1);
            topicRepository.save(frontend1);

            Topic frontend2 = TestDataFactory.createTestTopic("frontend");
            frontend2.setTitle("CSS Grid 용어");
            frontend2.setOrder(2);
            topicRepository.save(frontend2);

            Topic backend1 = TestDataFactory.createTestTopic("backend");
            backend1.setTitle("API 용어");
            topicRepository.save(backend1);

            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "frontend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].domain").value("frontend"))
                    .andExpect(jsonPath("$.data[1].domain").value("frontend"));
        }

        @Test
        @DisplayName("should return empty list for domain with no topics")
        void shouldReturnEmptyForNoDomain() throws Exception {
            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "agile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("should only return active topics")
        void shouldOnlyReturnActiveTopics() throws Exception {
            Topic active = TestDataFactory.createTestTopic("frontend");
            active.setActive(true);
            topicRepository.save(active);

            Topic inactive = TestDataFactory.createTestTopic("frontend");
            inactive.setActive(false);
            topicRepository.save(inactive);

            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "frontend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("should return skill tree with completion data")
        void shouldReturnSkillTreeWithCompletionData() throws Exception {
            Topic topic = TestDataFactory.createTestTopic("frontend");
            topic = topicRepository.save(topic);

            // Create lessons for this topic
            lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 1));
            lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 2));
            lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 3));

            // Create progress
            LearningProgress progress = TestDataFactory.createLearningProgress(
                    savedUser.getId(), topic.getId(), "frontend");
            progressRepository.save(progress);

            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "frontend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].completionPercent").value(40.0))
                    .andExpect(jsonPath("$.data[0].completedLessons").value(2))
                    .andExpect(jsonPath("$.data[0].totalLessons").value(3));
        }

        @Test
        @DisplayName("should mark topics as locked when prerequisites are incomplete")
        void shouldMarkTopicsAsLockedWhenPrereqsIncomplete() throws Exception {
            // Topic A: no prerequisites
            Topic topicA = TestDataFactory.createTestTopic("frontend");
            topicA.setTitle("HTML Basics");
            topicA.setOrder(1);
            topicA = topicRepository.save(topicA);

            // Topic B: depends on Topic A (not completed)
            Topic topicB = TestDataFactory.createTestTopicWithPrerequisites(
                    "frontend", List.of(topicA.getId()));
            topicB.setTitle("CSS Grid");
            topicB = topicRepository.save(topicB);

            // No progress for Topic A → Topic B should be locked

            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "frontend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].locked").value(false))  // Topic A — no prereqs
                    .andExpect(jsonPath("$.data[1].locked").value(true));  // Topic B — prereq incomplete
        }

        @Test
        @DisplayName("should unlock topic when all prerequisites are completed")
        void shouldUnlockTopicWhenPrereqsCompleted() throws Exception {
            Topic topicA = TestDataFactory.createTestTopic("frontend");
            topicA.setTitle("HTML Basics");
            topicA.setOrder(1);
            topicA = topicRepository.save(topicA);

            Topic topicB = TestDataFactory.createTestTopicWithPrerequisites(
                    "frontend", List.of(topicA.getId()));
            topicB.setTitle("CSS Grid");
            topicB = topicRepository.save(topicB);

            // Complete prerequisite
            LearningProgress completeProgress = TestDataFactory.createCompletedProgress(
                    savedUser.getId(), topicA.getId(), "frontend");
            progressRepository.save(completeProgress);

            mockMvc.perform(get("/api/topics")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("domain", "frontend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].locked").value(false))   // Topic A
                    .andExpect(jsonPath("$.data[1].locked").value(false));  // Topic B — prereq complete
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/topics")
                            .param("domain", "frontend"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===========================================
    //  Lesson Controller Tests
    // ===========================================

    @Nested
    @DisplayName("GET /api/lessons")
    class GetLessonsByTopic {

        @Test
        @DisplayName("should return lessons for a topic ordered by order field")
        void shouldReturnLessonsForTopic() throws Exception {
            Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));

            Lesson lesson1 = TestDataFactory.createTestLesson(topic.getId(), 1);
            Lesson lesson2 = TestDataFactory.createTestLesson(topic.getId(), 2);
            Lesson lesson3 = TestDataFactory.createTestLesson(topic.getId(), 3);
            lessonRepository.saveAll(List.of(lesson3, lesson1, lesson2)); // Save out of order

            mockMvc.perform(get("/api/lessons")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("topicId", topic.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].order").value(1))
                    .andExpect(jsonPath("$.data[1].order").value(2))
                    .andExpect(jsonPath("$.data[2].order").value(3));
        }

        @Test
        @DisplayName("should return empty list for topic with no lessons")
        void shouldReturnEmptyForNoLessons() throws Exception {
            Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));

            mockMvc.perform(get("/api/lessons")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("topicId", topic.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/lessons/{id}")
    class GetLessonDetail {

        @Test
        @DisplayName("should return lesson detail with vocabulary and exercises")
        void shouldReturnLessonWithVocabAndExercises() throws Exception {
            Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));
            Lesson lesson = lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 1));

            mockMvc.perform(get("/api/lessons/{id}", lesson.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").isNotEmpty())
                    .andExpect(jsonPath("$.data.titleVi").isNotEmpty())
                    .andExpect(jsonPath("$.data.vocabulary", hasSize(2)))
                    .andExpect(jsonPath("$.data.vocabulary[0].korean").value("방향"))
                    .andExpect(jsonPath("$.data.vocabulary[0].pronunciation").value("banghyang"))
                    .andExpect(jsonPath("$.data.exercises", hasSize(1)))
                    .andExpect(jsonPath("$.data.exercises[0].type").value("multiple_choice"));
        }

        @Test
        @DisplayName("should return 404 for non-existent lesson")
        void shouldReturn404ForNonExistentLesson() throws Exception {
            mockMvc.perform(get("/api/lessons/{id}", "nonexistent_id")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}

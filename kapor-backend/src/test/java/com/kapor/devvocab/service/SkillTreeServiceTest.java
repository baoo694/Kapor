package com.kapor.devvocab.service;

import com.kapor.TestDataFactory;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.dto.SkillNodeDto;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SkillTreeService}.
 * Tests skill tree building, prerequisite checking, and progress mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SkillTree Service Unit Tests")
class SkillTreeServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningProgressRepository progressRepository;

    @InjectMocks
    private SkillTreeService skillTreeService;

    @Nested
    @DisplayName("Skill Tree Building")
    class SkillTreeBuilding {

        @Test
        @DisplayName("should build skill tree with correct completion percentages")
        void shouldBuildSkillTreeWithCompletionPercentages() {
            Topic topic = TestDataFactory.createTestTopic("frontend");
            topic.setId("topic_1");
            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("frontend"))
                    .thenReturn(List.of(topic));
            when(lessonRepository.countByTopicId("topic_1")).thenReturn(5L);

            LearningProgress progress = TestDataFactory.createLearningProgress("user_1", "topic_1", "frontend");
            when(progressRepository.findByUserIdAndDomain("user_1", "frontend"))
                    .thenReturn(List.of(progress));

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "frontend");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCompletionPercent()).isEqualTo(40.0);
            assertThat(result.get(0).getCompletedLessons()).isEqualTo(2);
            assertThat(result.get(0).getTotalLessons()).isEqualTo(5);
        }

        @Test
        @DisplayName("should return 0% completion for unstarted topics")
        void shouldReturnZeroCompletionForUnstartedTopics() {
            Topic topic = TestDataFactory.createTestTopic("frontend");
            topic.setId("topic_new");
            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("frontend"))
                    .thenReturn(List.of(topic));
            when(lessonRepository.countByTopicId("topic_new")).thenReturn(3L);
            when(progressRepository.findByUserIdAndDomain("user_1", "frontend"))
                    .thenReturn(Collections.emptyList());

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "frontend");

            assertThat(result.get(0).getCompletionPercent()).isEqualTo(0.0);
            assertThat(result.get(0).getCompletedLessons()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty list when no active topics exist")
        void shouldReturnEmptyListWhenNoTopics() {
            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("devops"))
                    .thenReturn(Collections.emptyList());

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "devops");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Prerequisite Locking")
    class PrerequisiteLocking {

        @Test
        @DisplayName("should lock topic when prerequisites are not completed")
        void shouldLockWhenPrereqsNotCompleted() {
            Topic prereq = TestDataFactory.createTestTopic("frontend");
            prereq.setId("prereq_1");
            prereq.setOrder(1);

            Topic dependent = TestDataFactory.createTestTopicWithPrerequisites("frontend", List.of("prereq_1"));
            dependent.setId("dependent_1");

            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("frontend"))
                    .thenReturn(List.of(prereq, dependent));
            when(lessonRepository.countByTopicId(anyString())).thenReturn(3L);
            when(progressRepository.findByUserIdAndDomain("user_1", "frontend"))
                    .thenReturn(Collections.emptyList()); // No progress at all

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "frontend");

            assertThat(result.get(0).isLocked()).isFalse(); // prereq has no prereqs
            assertThat(result.get(1).isLocked()).isTrue();   // dependent's prereq incomplete
        }

        @Test
        @DisplayName("should unlock topic when all prerequisites are 100% completed")
        void shouldUnlockWhenPrereqsCompleted() {
            Topic prereq = TestDataFactory.createTestTopic("frontend");
            prereq.setId("prereq_1");
            prereq.setOrder(1);

            Topic dependent = TestDataFactory.createTestTopicWithPrerequisites("frontend", List.of("prereq_1"));
            dependent.setId("dependent_1");

            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("frontend"))
                    .thenReturn(List.of(prereq, dependent));
            when(lessonRepository.countByTopicId(anyString())).thenReturn(5L);

            LearningProgress completedProgress = TestDataFactory.createCompletedProgress("user_1", "prereq_1", "frontend");
            when(progressRepository.findByUserIdAndDomain("user_1", "frontend"))
                    .thenReturn(List.of(completedProgress));

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "frontend");

            assertThat(result.get(1).isLocked()).isFalse(); // prereq is completed → unlock
        }

        @Test
        @DisplayName("should keep topic locked when prereq is partially completed")
        void shouldKeepLockedWhenPrereqPartiallyCompleted() {
            Topic prereq = TestDataFactory.createTestTopic("frontend");
            prereq.setId("prereq_1");
            prereq.setOrder(1);

            Topic dependent = TestDataFactory.createTestTopicWithPrerequisites("frontend", List.of("prereq_1"));
            dependent.setId("dependent_1");

            when(topicRepository.findByDomainAndIsActiveTrueOrderByOrderAsc("frontend"))
                    .thenReturn(List.of(prereq, dependent));
            when(lessonRepository.countByTopicId(anyString())).thenReturn(5L);

            // prereq only 60% done — not enough
            LearningProgress partialProgress = TestDataFactory.createLearningProgress("user_1", "prereq_1", "frontend");
            partialProgress.setCompletionPercent(60.0);
            when(progressRepository.findByUserIdAndDomain("user_1", "frontend"))
                    .thenReturn(List.of(partialProgress));

            List<SkillNodeDto> result = skillTreeService.getSkillTree("user_1", "frontend");

            assertThat(result.get(1).isLocked()).isTrue(); // Still locked
        }
    }
}

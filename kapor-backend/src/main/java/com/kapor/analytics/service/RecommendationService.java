package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.user.model.User;
import com.kapor.user.model.LearningGoal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MembyteFlashcardRepository flashcardRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TopicRepository topicRepository;

    public DashboardResponse.RecommendationCard generateRecommendation(User user) {
        Set<String> goals = LearningGoal.normalize(user.getProfile() == null ? List.of() : user.getProfile().getLearningGoals());
        Instant now = Instant.now();
        int dueCards = (int) flashcardRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .filter(card -> !card.isNew() && card.getDueAt() != null && !card.getDueAt().isAfter(now))
                .count();
        if (dueCards > 0) {
            return DashboardResponse.RecommendationCard.builder()
                    .type("review_due")
                    .title("Ôn tập thẻ đến hạn")
                    .subtitle("Bạn có " + dueCards + " thẻ MemByte cần ôn tập hôm nay")
                    .targetScreen("/membyte-review/all")
                    .icon("🧠")
                    .build();
        }

        LearningProgress progress = learningProgressRepository.findByUserId(user.getId()).stream()
                .filter(LearningProgress::isUnlocked)
                .filter(item -> item.getTotalLessons() > 0 && item.getCompletedLessons() < item.getTotalLessons())
                .filter(item -> topicRepository.findById(item.getTopicId()).map(topic -> matchesGoals(topic, goals)).orElse(false))
                .max(Comparator.comparing(LearningProgress::getLastAccessedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (progress != null) {
            Topic topic = topicRepository.findById(progress.getTopicId()).orElse(null);
            String title = topic == null ? "Tiếp tục DevVocab" : title(topic);
            return DashboardResponse.RecommendationCard.builder()
                    .type("resume_lesson")
                    .title(title)
                    .subtitle("Bạn đã hoàn thành " + progress.getCompletedLessons() + "/" + progress.getTotalLessons() + " bài")
                    .targetScreen("/devvocab-topic/" + progress.getTopicId())
                    .icon("⚡")
                    .build();
        }

        Topic firstActiveTopic = topicRepository.findAllByOrderByOrderAsc().stream()
                .filter(Topic::isActive)
                .filter(topic -> matchesGoals(topic, goals))
                .findFirst()
                .orElse(null);
        if (firstActiveTopic != null) {
            return DashboardResponse.RecommendationCard.builder()
                    .type("new_topic")
                    .title(title(firstActiveTopic))
                    .subtitle("Bắt đầu chủ đề tiếp theo của bạn")
                    .targetScreen("/devvocab-topic/" + firstActiveTopic.getId())
                    .icon("📚")
                    .build();
        }

        return DashboardResponse.RecommendationCard.builder()
                .type("explore")
                .title("Khám phá DevVocab")
                .subtitle("Chọn một chủ đề để bắt đầu hành trình học")
                .targetScreen("/devvocab")
                .icon("📚")
                .build();
    }

    private String title(Topic topic) {
        return topic.getTitleVi() == null || topic.getTitleVi().isBlank() ? topic.getTitle() : topic.getTitleVi();
    }

    private boolean matchesGoals(Topic topic, Set<String> goals) {
        if (goals.isEmpty()) return true;
        if (topic.getGoalTags() != null && topic.getGoalTags().stream().anyMatch(goals::contains)) return true;
        // Existing DevVocab topics predate goal tags; they are still useful for
        // learners who explicitly chose Korean IT terminology.
        return goals.contains(LearningGoal.IT_TERMINOLOGY.value())
                && topic.getDomain() != null && !topic.getDomain().isBlank();
    }
}

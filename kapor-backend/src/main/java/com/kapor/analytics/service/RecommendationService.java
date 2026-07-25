package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MembyteFlashcardRepository flashcardRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TopicRepository topicRepository;

    public DashboardResponse.RecommendationCard generateRecommendation(User user) {
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
}

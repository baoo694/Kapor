package com.kapor.devvocab.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.devvocab.dto.FlashcardProgressDto;
import com.kapor.devvocab.model.FlashcardProgress;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.FlashcardProgressRepository;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashcardProgressService {

    private final FlashcardProgressRepository flashcardProgressRepository;
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final LearningProgressRepository learningProgressRepository;

    public FlashcardProgressDto getProgress(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        return toDto(lesson, flashcardProgressRepository.findByUserIdAndLessonId(userId, lessonId));
    }

    public FlashcardProgressDto updateStatus(
            String userId,
            String lessonId,
            String vocabularyId,
            FlashcardProgress.Status status) {
        Lesson lesson = getLesson(lessonId);
        boolean vocabularyExists = lesson.getVocabulary() != null && lesson.getVocabulary().stream()
                .anyMatch(item -> vocabularyId.equals(item.getId()));
        if (!vocabularyExists) {
            throw new ResourceNotFoundException("Vocabulary", "id", vocabularyId);
        }

        FlashcardProgress progress = flashcardProgressRepository
                .findByUserIdAndLessonIdAndVocabularyId(userId, lessonId, vocabularyId)
                .orElseGet(() -> FlashcardProgress.builder()
                        .userId(userId)
                        .lessonId(lessonId)
                        .vocabularyId(vocabularyId)
                        .build());
        progress.setStatus(status);
        flashcardProgressRepository.save(progress);
        syncTopicProgress(userId, lesson);

        return getProgress(userId, lessonId);
    }

    public FlashcardProgressDto resetProgress(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        flashcardProgressRepository.deleteByUserIdAndLessonId(userId, lessonId);
        syncTopicProgress(userId, lesson);
        return toDto(lesson, List.of());
    }

    private Lesson getLesson(String lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private FlashcardProgressDto toDto(Lesson lesson, List<FlashcardProgress> progressEntries) {
        Set<String> vocabularyIds = new HashSet<>();
        if (lesson.getVocabulary() != null) {
            lesson.getVocabulary().forEach(item -> vocabularyIds.add(item.getId()));
        }

        Map<String, FlashcardProgress.Status> statuses = new HashMap<>();
        progressEntries.stream()
                .filter(entry -> vocabularyIds.contains(entry.getVocabularyId()))
                .filter(entry -> entry.getStatus() != null)
                .forEach(entry -> statuses.put(entry.getVocabularyId(), entry.getStatus()));

        int knownCards = (int) statuses.values().stream()
                .filter(status -> status == FlashcardProgress.Status.KNOWN)
                .count();
        int learningCards = (int) statuses.values().stream()
                .filter(status -> status == FlashcardProgress.Status.LEARNING)
                .count();

        return FlashcardProgressDto.builder()
                .lessonId(lesson.getId())
                .totalCards(vocabularyIds.size())
                .knownCards(knownCards)
                .learningCards(learningCards)
                .cardStatuses(statuses)
                .build();
    }

    private void syncTopicProgress(String userId, Lesson changedLesson) {
        Topic topic = topicRepository.findById(changedLesson.getTopicId()).orElse(null);
        if (topic == null) return;

        List<Lesson> lessons = lessonRepository.findByTopicIdOrderByOrderAsc(topic.getId());
        if (lessons.isEmpty()) return;
        java.util.Map<String, List<FlashcardProgress>> progressByLesson = flashcardProgressRepository.findByUserId(userId)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(FlashcardProgress::getLessonId));
        int completedLessons = (int) lessons.stream()
                .filter(lesson -> isLessonCompleted(lesson, progressByLesson.getOrDefault(lesson.getId(), List.of())))
                .count();
        int totalLessons = lessons.size();
        double completion = totalLessons == 0 ? 0 : completedLessons * 100.0 / totalLessons;

        LearningProgress progress = learningProgressRepository.findByUserIdAndTopicId(userId, topic.getId())
                .orElseGet(() -> LearningProgress.builder()
                        .userId(userId)
                        .topicId(topic.getId())
                        .domain(topic.getDomain())
                        .isUnlocked(true)
                        .build());
        progress.setCompletedLessons(completedLessons);
        progress.setTotalLessons(totalLessons);
        progress.setCompletionPercent(completion);
        learningProgressRepository.save(progress);
    }

    private boolean isLessonCompleted(Lesson lesson, List<FlashcardProgress> entries) {
        if (lesson.getVocabulary() == null || lesson.getVocabulary().isEmpty()) return false;
        Set<String> knownVocabulary = entries.stream()
                .filter(entry -> entry.getStatus() == FlashcardProgress.Status.KNOWN)
                .map(FlashcardProgress::getVocabularyId)
                .collect(java.util.stream.Collectors.toSet());
        return lesson.getVocabulary().stream().allMatch(item -> knownVocabulary.contains(item.getId()));
    }
}

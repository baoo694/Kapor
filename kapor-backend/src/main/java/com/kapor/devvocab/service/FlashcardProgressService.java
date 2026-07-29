package com.kapor.devvocab.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.dto.FlashcardProgressDto;
import com.kapor.devvocab.model.FlashcardProgress;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.repository.FlashcardProgressRepository;
import com.kapor.devvocab.repository.LessonRepository;
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
    private final LessonActivityProgressService lessonActivityProgressService;

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
        lessonActivityProgressService.assertLessonIsUnlocked(userId, lesson);
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
        lessonActivityProgressService.syncTopicProgress(userId, lesson.getTopicId());

        return getProgress(userId, lessonId);
    }

    public FlashcardProgressDto resetProgress(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        flashcardProgressRepository.deleteByUserIdAndLessonId(userId, lessonId);
        lessonActivityProgressService.syncTopicProgress(userId, lesson.getTopicId());
        return toDto(lesson, List.of());
    }

    public FlashcardProgressDto resetCardProgress(
            String userId, String lessonId, String vocabularyId) {
        Lesson lesson = getLesson(lessonId);
        boolean vocabularyExists = lesson.getVocabulary() != null && lesson.getVocabulary().stream()
                .anyMatch(item -> vocabularyId.equals(item.getId()));
        if (!vocabularyExists) {
            throw new ResourceNotFoundException("Vocabulary", "id", vocabularyId);
        }
        flashcardProgressRepository.deleteByUserIdAndLessonIdAndVocabularyId(
                userId, lessonId, vocabularyId);
        lessonActivityProgressService.syncTopicProgress(userId, lesson.getTopicId());
        return getProgress(userId, lessonId);
    }

    /**
     * Returns a lesson only when every earlier lesson in the same topic has
     * been completed. Keep this check in the service so deep links and direct
     * API calls cannot bypass the lock displayed by the client.
     */
    public Lesson getUnlockedLesson(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        lessonActivityProgressService.assertLessonIsUnlocked(userId, lesson);
        return lesson;
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

}

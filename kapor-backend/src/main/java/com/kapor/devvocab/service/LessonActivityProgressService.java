package com.kapor.devvocab.service;

import com.kapor.analytics.model.LearningProgress;
import com.kapor.analytics.repository.LearningProgressRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.dto.LessonActivityProgressDto;
import com.kapor.devvocab.dto.MatchingAttemptRequest;
import com.kapor.devvocab.dto.QuizResultDto;
import com.kapor.devvocab.dto.QuizSubmissionRequest;
import com.kapor.devvocab.model.FlashcardProgress;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.LessonActivityProgress;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.FlashcardProgressRepository;
import com.kapor.devvocab.repository.LessonActivityProgressRepository;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonActivityProgressService {

    private static final int QUIZ_PASSING_SCORE = 80;

    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final FlashcardProgressRepository flashcardProgressRepository;
    private final LessonActivityProgressRepository activityProgressRepository;
    private final LearningProgressRepository learningProgressRepository;

    public LessonActivityProgressDto getProgress(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        return toDto(userId, lesson, activityProgressRepository
                .findByUserIdAndLessonId(userId, lessonId).orElse(null));
    }

    public LessonActivityProgressDto completeStudy(String userId, String lessonId) {
        Lesson lesson = getLesson(lessonId);
        assertLessonIsUnlocked(userId, lesson);
        LessonActivityProgress progress = getOrCreate(userId, lessonId);
        progress.setStudyCompleted(true);
        progress.setStudyCompletedAt(Instant.now());
        activityProgressRepository.save(progress);
        syncTopicProgress(userId, lesson.getTopicId());
        return toDto(userId, lesson, progress);
    }

    public QuizResultDto submitQuiz(String userId, String lessonId, QuizSubmissionRequest request) {
        Lesson lesson = getLesson(lessonId);
        assertLessonIsUnlocked(userId, lesson);
        LessonActivityProgress progress = getOrCreate(userId, lessonId);
        if (!progress.isStudyCompleted()) {
            throw new AccessDeniedException("Hoàn thành phần Học trước khi làm Kiểm tra.");
        }

        List<Lesson.Exercise> exercises = lesson.getExercises() == null ? List.of() : lesson.getExercises();
        if (exercises.isEmpty()) {
            throw new IllegalArgumentException("Bài học này chưa có câu hỏi kiểm tra.");
        }
        Map<String, String> answers = request.getAnswers();
        int correctAnswers = (int) exercises.stream()
                .filter(exercise -> exercise.getId() != null)
                .filter(exercise -> normalize(exercise.getCorrectAnswer())
                        .equals(normalize(answers.get(exercise.getId()))))
                .count();
        int score = (int) Math.round(correctAnswers * 100.0 / exercises.size());
        boolean passed = score >= QUIZ_PASSING_SCORE;

        progress.setQuizAttempts(progress.getQuizAttempts() + 1);
        progress.setBestQuizScore(Math.max(progress.getBestQuizScore(), score));
        progress.setQuizPassed(progress.isQuizPassed() || passed);
        activityProgressRepository.save(progress);
        syncTopicProgress(userId, lesson.getTopicId());

        return QuizResultDto.builder()
                .score(score)
                .correctAnswers(correctAnswers)
                .totalQuestions(exercises.size())
                .passed(passed)
                .progress(toDto(userId, lesson, progress))
                .build();
    }

    public LessonActivityProgressDto recordMatchingAttempt(
            String userId, String lessonId, MatchingAttemptRequest request) {
        Lesson lesson = getLesson(lessonId);
        assertLessonIsUnlocked(userId, lesson);
        int totalPairs = lesson.getVocabulary() == null ? 0 : lesson.getVocabulary().size();
        if (totalPairs == 0) {
            throw new IllegalArgumentException("Bài học này chưa có thẻ để ghép.");
        }
        if (request.getCompletedPairs() > totalPairs) {
            throw new IllegalArgumentException("Số cặp hoàn thành không hợp lệ.");
        }
        int accuracy = (int) Math.round(request.getCompletedPairs() * 100.0 / totalPairs);
        LessonActivityProgress progress = getOrCreate(userId, lessonId);
        progress.setMatchingAttempts(progress.getMatchingAttempts() + 1);
        progress.setBestMatchAccuracy(Math.max(progress.getBestMatchAccuracy(), accuracy));
        activityProgressRepository.save(progress);
        return toDto(userId, lesson, progress);
    }

    public void assertLessonIsUnlocked(String userId, Lesson lesson) {
        for (Lesson earlierLesson : lessonRepository.findByTopicIdOrderByOrderAsc(lesson.getTopicId())) {
            if (earlierLesson.getId().equals(lesson.getId())) return;
            if (!isLessonCompleted(userId, earlierLesson)) {
                throw new AccessDeniedException("Hoàn thành bài học trước để mở khóa bài học này.");
            }
        }
    }

    public boolean isLessonCompleted(String userId, Lesson lesson) {
        LessonActivityProgress progress = activityProgressRepository
                .findByUserIdAndLessonId(userId, lesson.getId()).orElse(null);
        if (progress != null) {
            return progress.isStudyCompleted() && progress.isQuizPassed();
        }
        return isLegacyFlashcardCompletion(userId, lesson);
    }

    public void syncTopicProgress(String userId, String topicId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return;
        List<Lesson> lessons = lessonRepository.findByTopicIdOrderByOrderAsc(topicId);
        int completedLessons = (int) lessons.stream()
                .filter(lesson -> isLessonCompleted(userId, lesson))
                .count();
        LearningProgress progress = learningProgressRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> LearningProgress.builder()
                        .userId(userId).topicId(topicId).domain(topic.getDomain()).isUnlocked(true).build());
        progress.setCompletedLessons(completedLessons);
        progress.setTotalLessons(lessons.size());
        progress.setCompletionPercent(lessons.isEmpty() ? 0 : completedLessons * 100.0 / lessons.size());
        learningProgressRepository.save(progress);
    }

    private Lesson getLesson(String lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private LessonActivityProgress getOrCreate(String userId, String lessonId) {
        return activityProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> LessonActivityProgress.builder().userId(userId).lessonId(lessonId).build());
    }

    private LessonActivityProgressDto toDto(String userId, Lesson lesson, LessonActivityProgress progress) {
        return LessonActivityProgressDto.builder()
                .lessonId(lesson.getId())
                .studyCompleted(progress != null && progress.isStudyCompleted())
                .quizPassed(progress != null && progress.isQuizPassed())
                .lessonCompleted(isLessonCompleted(userId, lesson))
                .bestQuizScore(progress == null ? 0 : progress.getBestQuizScore())
                .quizAttempts(progress == null ? 0 : progress.getQuizAttempts())
                .bestMatchAccuracy(progress == null ? 0 : progress.getBestMatchAccuracy())
                .matchingAttempts(progress == null ? 0 : progress.getMatchingAttempts())
                .build();
    }

    private boolean isLegacyFlashcardCompletion(String userId, Lesson lesson) {
        if (lesson.getVocabulary() == null || lesson.getVocabulary().isEmpty()) return false;
        Set<String> knownVocabulary = flashcardProgressRepository.findByUserIdAndLessonId(userId, lesson.getId())
                .stream()
                .filter(entry -> entry.getStatus() == FlashcardProgress.Status.KNOWN)
                .map(FlashcardProgress::getVocabularyId)
                .collect(Collectors.toSet());
        return lesson.getVocabulary().stream().allMatch(item -> knownVocabulary.contains(item.getId()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

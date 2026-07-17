package com.kapor.admin.service;

import com.kapor.admin.dto.AdminLessonDto;
import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminLessonService {

    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;

    public List<AdminLessonDto> findAll(String topicId) {
        List<Lesson> lessons = topicId == null || topicId.isBlank()
                ? lessonRepository.findAllByOrderByOrderAsc()
                : lessonRepository.findByTopicIdOrderByOrderAsc(topicId.trim());
        Map<String, Topic> topicsById = new HashMap<>();
        topicRepository.findAll().forEach(topic -> topicsById.put(topic.getId(), topic));
        return lessons.stream().map(lesson -> toDto(lesson, topicsById.get(lesson.getTopicId()))).toList();
    }

    public AdminLessonDto create(AdminLessonDto dto) {
        Topic topic = requireTopic(dto.getTopicId());
        Lesson lesson = new Lesson();
        apply(dto, lesson);
        return toDto(lessonRepository.save(lesson), topic);
    }

    public AdminLessonDto update(String id, AdminLessonDto dto) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        Topic topic = requireTopic(dto.getTopicId());
        apply(dto, lesson);
        return toDto(lessonRepository.save(lesson), topic);
    }

    public void deleteById(String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lessonRepository.delete(lesson);
    }

    private Topic requireTopic(String topicId) {
        return topicRepository.findById(topicId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));
    }

    private void apply(AdminLessonDto dto, Lesson lesson) {
        lesson.setTopicId(dto.getTopicId().trim());
        lesson.setTitle(dto.getTitle().trim());
        lesson.setTitleVi(dto.getTitleVi().trim());
        lesson.setContent(trimToNull(dto.getContent()));
        lesson.setContentVi(trimToNull(dto.getContentVi()));
        lesson.setOrder(dto.getOrder());
        lesson.setVocabulary(normalizeVocabulary(dto.getVocabulary()));
        lesson.setExercises(normalizeExercises(dto.getExercises()));
    }

    private List<Lesson.VocabularyItem> normalizeVocabulary(List<Lesson.VocabularyItem> vocabulary) {
        List<Lesson.VocabularyItem> items = vocabulary == null ? new ArrayList<>() : new ArrayList<>(vocabulary);
        items.forEach(item -> {
            if (item.getId() == null || item.getId().isBlank()) {
                item.setId(UUID.randomUUID().toString());
            }
        });
        return items;
    }

    private List<Lesson.Exercise> normalizeExercises(List<Lesson.Exercise> exercises) {
        List<Lesson.Exercise> items = exercises == null ? new ArrayList<>() : new ArrayList<>(exercises);
        items.forEach(item -> {
            if (item.getId() == null || item.getId().isBlank()) {
                item.setId(UUID.randomUUID().toString());
            }
        });
        return items;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AdminLessonDto toDto(Lesson lesson, Topic topic) {
        return AdminLessonDto.builder()
                .id(lesson.getId())
                .topicId(lesson.getTopicId())
                .title(lesson.getTitle())
                .titleVi(lesson.getTitleVi())
                .content(lesson.getContent())
                .contentVi(lesson.getContentVi())
                .order(lesson.getOrder())
                .vocabulary(lesson.getVocabulary() == null ? new ArrayList<>() : new ArrayList<>(lesson.getVocabulary()))
                .exercises(lesson.getExercises() == null ? new ArrayList<>() : new ArrayList<>(lesson.getExercises()))
                .topicTitle(topic == null ? null : topic.getTitle())
                .topicTitleVi(topic == null ? null : topic.getTitleVi())
                .domain(topic == null ? null : topic.getDomain())
                .build();
    }
}

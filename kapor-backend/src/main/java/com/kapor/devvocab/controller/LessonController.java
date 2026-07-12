package com.kapor.devvocab.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonRepository lessonRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessonsByTopic(@RequestParam String topicId) {
        List<Lesson> lessons = lessonRepository.findByTopicIdOrderByOrderAsc(topicId);
        return ResponseEntity.ok(ApiResponse.ok(lessons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lesson>> getLessonDetail(@PathVariable String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        return ResponseEntity.ok(ApiResponse.ok(lesson));
    }
}

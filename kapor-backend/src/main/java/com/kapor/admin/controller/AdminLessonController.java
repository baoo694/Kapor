package com.kapor.admin.controller;

import com.kapor.admin.dto.AdminLessonDto;
import com.kapor.admin.service.AdminLessonService;
import com.kapor.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class AdminLessonController {

    private final AdminLessonService adminLessonService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLessonDto>>> getLessons(
            @RequestParam(required = false) String topicId) {
        return ResponseEntity.ok(ApiResponse.ok(adminLessonService.findAll(topicId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminLessonDto>> createLesson(@Valid @RequestBody AdminLessonDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(adminLessonService.create(dto), "Lesson created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminLessonDto>> updateLesson(
            @PathVariable String id,
            @Valid @RequestBody AdminLessonDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(adminLessonService.update(id, dto), "Lesson updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable String id) {
        adminLessonService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Lesson deleted successfully"));
    }
}

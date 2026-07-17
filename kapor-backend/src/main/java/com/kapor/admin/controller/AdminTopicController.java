package com.kapor.admin.controller;

import com.kapor.admin.dto.TopicDto;
import com.kapor.admin.service.AdminTopicService;
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
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
public class AdminTopicController {

    private final AdminTopicService adminTopicService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TopicDto>>> getTopics(
            @RequestParam(required = false) String domain) {
        return ResponseEntity.ok(ApiResponse.ok(adminTopicService.findAll(domain)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TopicDto>> createTopic(@Valid @RequestBody TopicDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(adminTopicService.save(dto), "Topic created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicDto>> updateTopic(
            @PathVariable String id,
            @Valid @RequestBody TopicDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(adminTopicService.update(id, dto), "Topic updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable String id) {
        adminTopicService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Topic deleted successfully"));
    }
}

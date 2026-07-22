package com.kapor.techtalk.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.techtalk.model.TechTalkScenario;
import com.kapor.techtalk.service.RoleplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {
    private final RoleplayService roleplayService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TechTalkScenario>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.getScenarios()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TechTalkScenario>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(roleplayService.getScenario(id)));
    }
}

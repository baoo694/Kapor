package com.kapor.devvocab.controller;

import com.kapor.common.dto.ApiResponse;
import com.kapor.devvocab.dto.SkillNodeDto;
import com.kapor.devvocab.service.SkillTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final SkillTreeService skillTreeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillNodeDto>>> getTopicsByDomain(
            Authentication authentication,
            @RequestParam String domain) {
        String userId = ((com.kapor.auth.security.CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        List<SkillNodeDto> nodes = skillTreeService.getSkillTree(userId, domain);
        return ResponseEntity.ok(ApiResponse.ok(nodes));
    }
}

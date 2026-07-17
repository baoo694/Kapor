package com.kapor.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.TestDataFactory;
import com.kapor.admin.dto.TopicDto;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        topicRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(TestDataFactory.createAdminUser());
        adminToken = jwtService.generateToken(new CustomUserDetails(admin));
    }

    @AfterEach
    void cleanUp() {
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void managesTopicsAndSupportsDomainFilter() throws Exception {
        TopicDto request = TopicDto.builder()
                .domain("frontend")
                .title("React 상태 관리")
                .titleVi("Quản lý state React")
                .description("Learn React state management vocabulary")
                .order(2)
                .prerequisiteTopicIds(List.of("topic-1"))
                .tags(List.of("React", "state"))
                .isActive(true)
                .build();

        String response = mockMvc.perform(post("/api/admin/topics")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("React 상태 관리"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn().getResponse().getContentAsString();

        String topicId = objectMapper.readTree(response).at("/data/id").asText();

        mockMvc.perform(get("/api/admin/topics").param("domain", "frontend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tags[0]").value("React"));

        request.setTitle("React hooks trạng thái");
        request.setIsActive(false);
        mockMvc.perform(put("/api/admin/topics/{id}", topicId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("React hooks trạng thái"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        mockMvc.perform(delete("/api/admin/topics/{id}", topicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectsTopicManagementForNonAdminUsers() throws Exception {
        User user = userRepository.save(TestDataFactory.createTestUser());
        String userToken = jwtService.generateToken(new CustomUserDetails(user));

        mockMvc.perform(get("/api/admin/topics")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}

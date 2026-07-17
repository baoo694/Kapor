package com.kapor.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.TestDataFactory;
import com.kapor.admin.dto.AdminLessonDto;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
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
class AdminLessonControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TopicRepository topicRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
        User admin = userRepository.save(TestDataFactory.createAdminUser());
        adminToken = jwtService.generateToken(new CustomUserDetails(admin));
    }

    @AfterEach
    void cleanUp() {
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void managesLessonsMapsTopicFieldsAndAssignsNestedIds() throws Exception {
        Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));
        AdminLessonDto request = request(topic.getId());

        String response = mockMvc.perform(post("/api/admin/lessons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicTitle").value(topic.getTitle()))
                .andExpect(jsonPath("$.data.topicTitleVi").value(topic.getTitleVi()))
                .andExpect(jsonPath("$.data.domain").value("frontend"))
                .andExpect(jsonPath("$.data.vocabulary[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data.exercises[0].id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String lessonId = objectMapper.readTree(response).at("/data/id").asText();
        request.setTitle("Flexbox cập nhật");
        mockMvc.perform(put("/api/admin/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Flexbox cập nhật"));

        mockMvc.perform(get("/api/admin/lessons").param("topicId", topic.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(delete("/api/admin/topics/{id}", topic.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/admin/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingTopicAndNonAdminAccess() throws Exception {
        AdminLessonDto request = request("missing-topic");
        mockMvc.perform(post("/api/admin/lessons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        User user = userRepository.save(TestDataFactory.createTestUser());
        String userToken = jwtService.generateToken(new CustomUserDetails(user));
        mockMvc.perform(get("/api/admin/lessons")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private AdminLessonDto request(String topicId) {
        return AdminLessonDto.builder()
                .topicId(topicId)
                .title("Flexbox 방향")
                .titleVi("Hướng Flexbox")
                .content("Korean content")
                .contentVi("Vietnamese content")
                .order(0)
                .vocabulary(List.of(Lesson.VocabularyItem.builder().korean("방향").vietnamese("Hướng").build()))
                .exercises(List.of(Lesson.Exercise.builder().type("fill_blank").question("빈칸").correctAnswer("답").build()))
                .build();
    }
}

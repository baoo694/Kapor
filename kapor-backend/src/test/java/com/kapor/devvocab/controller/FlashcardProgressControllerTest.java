package com.kapor.devvocab.controller;

import com.kapor.TestDataFactory;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.FlashcardProgressRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlashcardProgressControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TopicRepository topicRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private FlashcardProgressRepository flashcardProgressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String token;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        flashcardProgressRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(TestDataFactory.createTestUser());
        token = jwtService.generateToken(new CustomUserDetails(user));
        Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));
        lesson = lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 0));
    }

    @AfterEach
    void cleanUp() {
        flashcardProgressRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void storesKnownAndLearningStatusForEachVocabularyCard() throws Exception {
        String firstVocabularyId = lesson.getVocabulary().get(0).getId();
        String secondVocabularyId = lesson.getVocabulary().get(1).getId();

        mockMvc.perform(get("/api/lessons/{id}/flashcards/progress", lesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCards").value(2))
                .andExpect(jsonPath("$.data.knownCards").value(0))
                .andExpect(jsonPath("$.data.learningCards").value(0));

        mockMvc.perform(put("/api/lessons/{id}/flashcards/{vocabularyId}", lesson.getId(), firstVocabularyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"KNOWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.knownCards").value(1))
                .andExpect(jsonPath("$.data.cardStatuses." + firstVocabularyId).value("KNOWN"));

        mockMvc.perform(put("/api/lessons/{id}/flashcards/{vocabularyId}", lesson.getId(), secondVocabularyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LEARNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.knownCards").value(1))
                .andExpect(jsonPath("$.data.learningCards").value(1));

        mockMvc.perform(delete("/api/lessons/{id}/flashcards/progress", lesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.knownCards").value(0))
                .andExpect(jsonPath("$.data.learningCards").value(0));
    }
}

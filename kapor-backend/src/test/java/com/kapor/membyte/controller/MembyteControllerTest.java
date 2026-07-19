package com.kapor.membyte.controller;

import com.kapor.TestDataFactory;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.membyte.repository.MembyteDeckRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MembyteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private MembyteDeckRepository deckRepository;
    @Autowired private MembyteFlashcardRepository flashcardRepository;

    private String token;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        flashcardRepository.deleteAll();
        deckRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(TestDataFactory.createTestUser());
        token = jwtService.generateToken(new CustomUserDetails(user));
        Topic topic = topicRepository.save(TestDataFactory.createTestTopic("frontend"));
        lesson = lessonRepository.save(TestDataFactory.createTestLesson(topic.getId(), 1));
    }

    @AfterEach
    void cleanUp() {
        flashcardRepository.deleteAll();
        deckRepository.deleteAll();
        lessonRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void savesStarredVocabularyIntoOneLessonDeckAndSchedulesReviewsWithFsrs() throws Exception {
        String firstVocabularyId = lesson.getVocabulary().get(0).getId();
        String secondVocabularyId = lesson.getVocabulary().get(1).getId();

        mockMvc.perform(post("/api/membyte/lessons/{lessonId}/flashcards/{vocabularyId}", lesson.getId(), firstVocabularyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadySaved").value(false));

        mockMvc.perform(post("/api/membyte/lessons/{lessonId}/flashcards/{vocabularyId}", lesson.getId(), secondVocabularyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/membyte/decks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title", containsString(lesson.getTitle())))
                .andExpect(jsonPath("$.data[0].totalCards").value(2))
                .andExpect(jsonPath("$.data[0].newCards").value(2))
                .andExpect(jsonPath("$.data[0].dueCards").value(0));

        String deckId = deckRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/membyte/review/cards")
                        .header("Authorization", "Bearer " + token)
                        .param("deckId", deckId)
                        .param("includeNew", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].nextReviewTimes.GOOD").value("1d"));

        String cardId = flashcardRepository.findAll().get(0).getId();
        mockMvc.perform(post("/api/membyte/review/rate")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"cardId\":\"" + cardId + "\",\"rating\":\"GOOD\",\"responseTimeMs\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextReviewLabel").value("1d"));

        var dueCard = flashcardRepository.findAll().stream()
                .filter(card -> !card.getId().equals(cardId))
                .findFirst()
                .orElseThrow();
        dueCard.setNew(false);
        dueCard.setDueAt(Instant.now().minusSeconds(60));
        flashcardRepository.save(dueCard);

        mockMvc.perform(get("/api/membyte/review/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDueCards").value(1))
                .andExpect(jsonPath("$.data.decksWithDueCards").value(1));

        mockMvc.perform(get("/api/membyte/review/cards").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}

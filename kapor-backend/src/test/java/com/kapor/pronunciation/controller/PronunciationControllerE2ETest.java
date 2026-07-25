package com.kapor.pronunciation.controller;

import com.kapor.TestDataFactory;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.pronunciation.model.PronunciationAttempt;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.repository.PronunciationAttemptRepository;
import com.kapor.pronunciation.repository.PronunciationExerciseRepository;
import com.kapor.pronunciation.service.PronunciationAssessmentProvider;
import com.kapor.pronunciation.service.PronunciationAudioStorage;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the authenticated multipart upload -> provider result -> protected playback flow. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PronunciationControllerE2ETest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PronunciationExerciseRepository exerciseRepository;
    @Autowired private PronunciationAttemptRepository attemptRepository;
    @MockBean private PronunciationAssessmentProvider assessmentProvider;
    @MockBean private PronunciationAudioStorage audioStorage;

    private String token;
    private String otherUserToken;
    private PronunciationExercise exercise;

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAll();
        exerciseRepository.deleteAll();
        userRepository.deleteAll();
        User user = userRepository.save(TestDataFactory.createTestUser());
        token = jwtService.generateToken(new CustomUserDetails(user));
        User otherUser = TestDataFactory.createTestUser();
        otherUser.setEmail("other-pronunciation@kapor.test");
        otherUser = userRepository.save(otherUser);
        otherUserToken = jwtService.generateToken(new CustomUserDetails(otherUser));
        exercise = exerciseRepository.save(PronunciationExercise.builder()
                .title("배포 연습").titleVi("Luyện triển khai").domain("backend").difficulty("beginner")
                .order(1).createdAt(Instant.now()).sentences(List.of(PronunciationExercise.Sentence.builder()
                        .text("서버 배포가 완료되었습니다").translationVi("Triển khai server đã hoàn tất")
                        .waveformData(List.of(.1, .6, .2)).build())).build());
        when(audioStorage.storeAttempt(anyString(), any())).thenReturn("pronunciation/attempts/test/attempt.wav");
        when(audioStorage.read("pronunciation/attempts/test/attempt.wav"))
                .thenReturn(new PronunciationAudioStorage.AudioData(new byte[] {82, 73, 70, 70}, "audio/wav"));
        when(assessmentProvider.assess(anyString(), anyString(), any())).thenReturn(
                new PronunciationAssessmentProvider.Assessment(
                        PronunciationAttempt.Scores.builder().overall(91).accuracy(89).fluency(88).completeness(96).build(),
                        "서버 배포가 완료되었습니다",
                        List.of(PronunciationAttempt.WordFeedback.builder().text("배포가").score(72)
                                .accuracy("needs_practice").phonemeDetail("Whisper nhận dạng chưa khớp từ này.").build()),
                        PronunciationAttempt.Analysis.builder().summaryVi("Cần đọc rõ hơn cụm 배포가.")
                                .correctedText("서버 배포가 완료되었습니다").grammarNoteVi("").build()));
    }

    @AfterEach
    void cleanUp() {
        attemptRepository.deleteAll();
        exerciseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void evaluatesPcmPersistsPrivateAudioAndOnlyLetsTheOwnerPlayItBack() throws Exception {
        byte[] pcm16KhzMono = new byte[32_000]; // one second of signed 16-bit PCM
        MockMultipartFile audio = new MockMultipartFile(
                "audioFile", "attempt.pcm", "audio/pcm", pcm16KhzMono);

        mockMvc.perform(multipart("/api/pronunciation/evaluate")
                        .file(audio).param("exerciseId", exercise.getId()).param("sentenceIndex", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.scores.overall").value(91))
                .andExpect(jsonPath("$.data.analysis.summaryVi").value("Cần đọc rõ hơn cụm 배포가."))
                .andExpect(jsonPath("$.data.transcription[0].text").value("배포가"))
                .andExpect(jsonPath("$.data.attemptAudioUrl").exists());

        PronunciationAttempt attempt = attemptRepository.findAll().get(0);
        mockMvc.perform(get("/api/pronunciation/history")
                        .header("Authorization", "Bearer " + token)
                        .param("exerciseId", exercise.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attemptAudioUrl").exists())
                .andExpect(jsonPath("$.data[0].audioObjectKey").doesNotExist())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());

        mockMvc.perform(get("/api/pronunciation/attempts/{id}/audio", attempt.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.valueOf("audio/wav"), MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"));

        mockMvc.perform(get("/api/pronunciation/attempts/{id}/audio", attempt.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }
}

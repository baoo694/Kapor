package com.kapor.techtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.techtalk.model.RoleplaySession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRoleplayProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiRoleplayProvider provider =
            new GeminiRoleplayProvider(WebClient.builder(), objectMapper);

    @Test
    void structuredEvaluationSchemaRequiresGroundedCorrections() {
        JsonNode schema = ReflectionTestUtils.invokeMethod(provider, "turnEvaluationSchema");

        JsonNode properties = schema.path("properties");
        assertThat(properties.path("grammar").path("maximum").asInt()).isEqualTo(100);
        assertThat(properties.path("corrections").path("items").path("required").toString())
                .contains("\"original\"");
        assertThat(properties.path("objectives").path("items").path("required").toString())
                .contains("\"completed\"");
        assertThat(schema.path("required").toString()).contains("\"feedbackVi\"");
        assertThat(schema.path("required").toString()).contains("\"completionMessageKo\"");
    }

    @Test
    void rejectsHallucinatedCorrectionsAndClampsScores() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "grammar": 150,
                  "vocabulary": 82,
                  "politeness": -5,
                  "feedbackVi": "Nên dùng văn phong trang trọng.",
                  "usedRequiredVocabulary": ["장애", "không có"],
                  "completionMessageKo": "보고를 마쳤습니다.",
                  "objectives": [
                    {"objective":"장애를 보고합니다.","completed":true,"evidence":"장애 때문에"}
                  ],
                  "corrections": [
                    {"original":"문제 있어요","suggestion":"문제가 있습니다","type":"formality","noteVi":"Trang trọng hơn."},
                    {"original":"không hề có","suggestion":"무관","type":"grammar","noteVi":"Hallucinated."}
                  ]
                }
                """);

        RoleplaySession.Evaluation evaluation = ReflectionTestUtils.invokeMethod(
                provider, "parseTurnEvaluation", response, "장애 때문에 문제 있어요.");

        assertThat(evaluation.getGrammar()).isEqualTo(100);
        assertThat(evaluation.getPoliteness()).isZero();
        assertThat(evaluation.getCorrections()).singleElement()
                .satisfies(correction -> assertThat(correction.getSuggestion()).isEqualTo("문제가 있습니다"));
        assertThat(evaluation.getUsedRequiredVocabulary()).containsExactly("장애");
        assertThat(evaluation.getObjectives()).singleElement()
                .satisfies(objective -> assertThat(objective.isCompleted()).isTrue());
        assertThat(evaluation.getCompletionMessageKo()).isEqualTo("보고를 마쳤습니다.");
    }

    @Test
    void acceptsAJsonObjectWrappedInAMarkdownFence() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {"candidates":[{"content":{"parts":[{"text":"```json\\n{\\"grammar\\":90}\\n```"}]}}]}
                """);

        JsonNode parsed = ReflectionTestUtils.invokeMethod(provider, "structuredCandidate", response);

        assertThat(parsed.path("grammar").asInt()).isEqualTo(90);
    }

    @Test
    void reportsWhenGeminiTruncatesAStructuredResponse() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"{\\"grammar\\":90"}]}}]}
                """);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(provider, "structuredCandidate", response))
                .isInstanceOfSatisfying(RoleplayAiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("GEMINI_RESPONSE_TRUNCATED"))
                .hasMessage("Gemini truncated the structured evaluation before it was complete.");
    }

    @Test
    void disablesThinkingForGemini25StructuredEvaluations() {
        ReflectionTestUtils.setField(provider, "modelName", "gemini-2.5-flash");
        ReflectionTestUtils.setField(provider, "evaluationMaxOutputTokens", 2048);
        ReflectionTestUtils.setField(provider, "evaluationThinkingBudget", 0);

        ObjectNode request = ReflectionTestUtils.invokeMethod(provider, "structuredRequest", "evaluate", objectMapper.createObjectNode());

        assertThat(request.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(2048);
        assertThat(request.path("generationConfig").path("thinkingConfig").path("thinkingBudget").asInt())
                .isZero();
    }
}

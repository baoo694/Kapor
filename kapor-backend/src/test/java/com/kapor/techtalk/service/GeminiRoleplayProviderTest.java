package com.kapor.techtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.techtalk.model.RoleplaySession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(schema.path("required").toString()).contains("\"feedbackVi\"");
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
    }
}

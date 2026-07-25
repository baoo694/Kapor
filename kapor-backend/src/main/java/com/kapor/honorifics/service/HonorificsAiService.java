package com.kapor.honorifics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.honorifics.dto.CorrectionDiffDto;
import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gemini fallback for Korean sentences which are outside the deterministic
 * honorific rules. It deliberately returns the same DTO as the rule engine so
 * mobile clients do not need a separate AI-only flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HonorificsAiService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<String> VALID_LEVELS = Set.of("banmal", "heyohaet", "hasipsio");
    private static final Set<String> VALID_TYPES = Set.of(
            "particle", "honorific", "vocabulary", "grammar", "verb_ending", "pronoun");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    /**
     * AI is optional: the rule-based analyzer remains available when Gemini is
     * disabled, rate-limited, or returns malformed structured output.
     */
    public Optional<HonorificsAnalysisDto> analyze(String text, String targetLevel) {
        if (!isConfigured()) return Optional.empty();
        try {
            JsonNode response = webClientBuilder.build().post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody(text, targetLevel))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                            .bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new IllegalStateException("Gemini returned HTTP " + clientResponse.statusCode().value())))
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
            return Optional.of(parseResponse(response, text));
        } catch (RuntimeException exception) {
            log.warn("Gemini honorific fallback unavailable; returning rule-based result: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your-");
    }

    private ObjectNode requestBody(String text, String targetLevel) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode parts = root.putArray("contents").addObject().putArray("parts");
        parts.addObject().put("text", prompt(text, targetLevel));
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", 0.1);
        config.put("responseMimeType", "application/json");
        config.set("responseJsonSchema", responseSchema());
        return root;
    }

    private String prompt(String text, String targetLevel) {
        return """
                You are a Korean business-language editor. Analyze the Korean input for honorifics, politeness,
                grammar, particles, pronouns, vocabulary, and verb endings. The requested target style is %s
                (hasipsio means formal corporate Korean). Preserve the original meaning and technical terms.
                Return corrections only when a change is genuinely needed. Every correction.original must be an
                exact substring of the input. transformedText must be the complete corrected Korean text; when no
                correction is needed it must exactly equal the input. currentLevel describes the INPUT, using only
                banmal, heyohaet, or hasipsio. confidence is between 0 and 1. Write explanations in Vietnamese.
                Input: %s
                """.formatted(targetLevel, text);
    }

    private JsonNode responseSchema() {
        try {
            return objectMapper.readTree("""
                    {"type":"object","properties":{
                      "currentLevel":{"type":"string","enum":["banmal","heyohaet","hasipsio"]},
                      "confidence":{"type":"number"},
                      "corrections":{"type":"array","items":{"type":"object","properties":{
                        "original":{"type":"string"},"corrected":{"type":"string"},
                        "type":{"type":"string","enum":["particle","honorific","vocabulary","grammar","verb_ending","pronoun"]},
                        "explanation":{"type":"string"}
                      },"required":["original","corrected","type","explanation"]}},
                      "transformedText":{"type":"string"}
                    },"required":["currentLevel","confidence","corrections","transformedText"]}
                    """);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create Gemini honorific schema", exception);
        }
    }

    private HonorificsAnalysisDto parseResponse(JsonNode response, String source) {
        if (response == null) throw new IllegalStateException("Gemini returned no response");
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual()) throw new IllegalStateException("Gemini returned no structured honorific result");
        try {
            JsonNode result = objectMapper.readTree(textNode.asText());
            String transformedText = result.path("transformedText").asText("").trim();
            if (transformedText.isBlank() || transformedText.length() > 3000) {
                throw new IllegalStateException("Gemini returned an invalid transformed text");
            }
            List<CorrectionDiffDto> corrections = new ArrayList<>();
            for (JsonNode correction : result.path("corrections")) {
                String original = correction.path("original").asText("").trim();
                String corrected = correction.path("corrected").asText("").trim();
                if (original.isBlank() || corrected.isBlank() || !source.contains(original)) {
                    throw new IllegalStateException("Gemini returned a correction outside the input");
                }
                String type = correction.path("type").asText("grammar");
                corrections.add(CorrectionDiffDto.builder()
                        .original(original)
                        .corrected(corrected)
                        .type(VALID_TYPES.contains(type) ? type : "grammar")
                        .explanation(correction.path("explanation").asText("Điều chỉnh cho phù hợp ngữ cảnh công việc.").trim())
                        .build());
            }
            String level = result.path("currentLevel").asText("banmal");
            double confidence = Math.max(0, Math.min(1, result.path("confidence").asDouble(0.8)));
            return HonorificsAnalysisDto.builder()
                    .currentLevel(VALID_LEVELS.contains(level) ? level : "banmal")
                    .confidence(confidence)
                    .analysisSource("ai_fallback")
                    .corrections(corrections)
                    .transformedText(transformedText)
                    .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gemini returned invalid honorific JSON", exception);
        }
    }
}

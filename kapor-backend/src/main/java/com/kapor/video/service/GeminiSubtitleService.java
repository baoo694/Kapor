package com.kapor.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.video.model.Video;
import com.kapor.video.exception.GeminiApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calls Gemini with a strict JSON schema for Korean subtitle translation and tokenization. */
@Service
@RequiredArgsConstructor
public class GeminiSubtitleService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    public List<AnalysisLine> analyze(List<Video.SubtitleLine> subtitles) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini is not configured. Set GEMINI_API_KEY on the backend and restart it.");
        }
        JsonNode response;
        try {
            response = webClientBuilder.build().post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody(subtitles))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(JsonNode.class)
                            .defaultIfEmpty(objectMapper.createObjectNode())
                            .map(body -> geminiError(clientResponse.statusCode(), body)))
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "Unable to reach Gemini. Check the backend network connection and retry.", exception);
        }
        try {
            return parseResponse(response, subtitles.size());
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini returned an unusable subtitle analysis. Please retry.", exception);
        }
    }

    private ObjectNode requestBody(List<Video.SubtitleLine> subtitles) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ArrayNode parts = contents.addObject().putArray("parts");
        parts.addObject().put("text", prompt(subtitles));
        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        // The legacy generateContent REST endpoint expects these fields directly
        // under generationConfig. The newer responseFormat object is rejected by
        // this endpoint with HTTP 400 for the configured Developer API key.
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseJsonSchema", responseSchema());
        return root;
    }

    private String prompt(List<Video.SubtitleLine> subtitles) {
        ArrayNode input = objectMapper.createArrayNode();
        for (int index = 0; index < subtitles.size(); index++) {
            input.addObject().put("index", index).put("korean", subtitles.get(index).getText());
        }
        try {
            return """
                    You are a Korean language educator translating Korean video subtitles into natural Vietnamese.
                    For every input line, return exactly one result with the same index.
                    Translate the full sentence naturally, retaining technical terminology when relevant.
                    Tokenize Korean into useful learner-facing words or grammatical units. Each token surface must be an exact substring of the Korean input. Use the dictionary form for lemma, a concise POS tag, Revised Romanization pronunciation, a Vietnamese meaning, a short English meaning, a plain-English learner definition, and one natural Korean example sentence using the lemma. Add a grammar note only when it teaches a particle, ending, connective, or construction. Do not invent facts, do not add Markdown, and do not omit a line.
                    Input lines:
                    """ + objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare Gemini subtitle request", exception);
        }
    }

    private JsonNode responseSchema() {
        try {
            return objectMapper.readTree("""
                    {
                      "type":"object",
                      "properties":{
                        "lines":{"type":"array","items":{"type":"object","properties":{
                          "index":{"type":"integer"},
                          "vietnamese":{"type":"string"},
                          "tokens":{"type":"array","items":{"type":"object","properties":{
                            "surface":{"type":"string"},
                            "lemma":{"type":"string"},
                            "pos":{"type":"string"},
                            "pronunciation":{"type":"string"},
                            "meaningVi":{"type":"string"},
                            "meaningEn":{"type":"string"},
                            "definitionEn":{"type":"string"},
                            "exampleKo":{"type":"string"},
                            "grammarNote":{"type":["string","null"]},
                            "clickable":{"type":"boolean"}
                          },"required":["surface","lemma","pos","pronunciation","meaningVi","meaningEn","definitionEn","exampleKo","grammarNote","clickable"]}}
                        },"required":["index","vietnamese","tokens"]}}
                      },"required":["lines"]
                    }
                    """);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare Gemini response schema", exception);
        }
    }

    private List<AnalysisLine> parseResponse(JsonNode response, int expectedLines) {
        if (response == null) throw new IllegalStateException("Gemini returned an empty response");
        JsonNode text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isTextual()) throw new IllegalStateException("Gemini did not return subtitle analysis");
        try {
            JsonNode lines = objectMapper.readTree(text.asText()).path("lines");
            if (!lines.isArray() || lines.size() != expectedLines) {
                throw new IllegalStateException("Gemini returned an incomplete subtitle analysis");
            }
            List<AnalysisLine> result = new ArrayList<>();
            for (JsonNode line : lines) {
                int index = line.path("index").asInt(-1);
                String vietnamese = line.path("vietnamese").asText("").trim();
                if (index < 0 || index >= expectedLines || vietnamese.isBlank()) {
                    throw new IllegalStateException("Gemini returned an invalid subtitle line");
                }
                List<Video.TokenizedWord> tokens = new ArrayList<>();
                for (JsonNode token : line.path("tokens")) {
                    String surface = token.path("surface").asText("").trim();
                    if (surface.isBlank()) continue;
                    tokens.add(Video.TokenizedWord.builder()
                            .surface(surface)
                            .stem(token.path("lemma").asText(surface).trim())
                            .pos(token.path("pos").asText("unknown").trim())
                            .pronunciation(token.path("pronunciation").asText("").trim())
                            .meaningVi(token.path("meaningVi").asText("").trim())
                            .meaningEn(token.path("meaningEn").asText("").trim())
                            .definitionEn(token.path("definitionEn").asText("").trim())
                            .exampleKo(token.path("exampleKo").asText("").trim())
                            .grammarNote(token.path("grammarNote").isNull() ? null : token.path("grammarNote").asText("").trim())
                            .clickable(token.path("clickable").asBoolean(true))
                            .build());
                }
                result.add(new AnalysisLine(index, vietnamese, tokens));
            }
            result.sort(Comparator.comparingInt(AnalysisLine::index));
            for (int index = 0; index < result.size(); index++) {
                if (result.get(index).index() != index) throw new IllegalStateException("Gemini returned duplicate subtitle indexes");
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gemini returned invalid JSON", exception);
        }
    }

    private GeminiApiException geminiError(HttpStatusCode status, JsonNode body) {
        if (status.value() == 401 || status.value() == 403) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini rejected the API key. Check GEMINI_API_KEY and its API restrictions.");
        }
        if (status.value() == 404) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini model '" + modelName + "' is unavailable for this API key. Set GEMINI_MODEL to an available model.");
        }
        if (status.value() == 429) {
            return new GeminiApiException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini quota or rate limit reached. Wait a moment, then retry.");
        }
        String message = body.path("error").path("message").asText("").replaceAll("[\\r\\n]+", " ").trim();
        return new GeminiApiException(HttpStatus.BAD_GATEWAY,
                message.isBlank() ? "Gemini could not process the subtitle request." : "Gemini request was rejected: " + message);
    }

    public record AnalysisLine(int index, String vietnamese, List<Video.TokenizedWord> tokens) { }
}

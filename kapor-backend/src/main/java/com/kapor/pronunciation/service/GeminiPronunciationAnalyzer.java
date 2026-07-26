package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.pronunciation.exception.PronunciationAssessmentException;
import com.kapor.pronunciation.model.PronunciationAttempt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Text-only Vietnamese renderer. It is deliberately unable to construct a
 * pronunciation score, infer an acoustic error, or modify the transcript.
 */
@Service
public class GeminiPronunciationAnalyzer {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}") private String apiKey;
    @Value("${gemini.model-name:gemini-2.5-flash}") private String modelName;

    public GeminiPronunciationAnalyzer(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public PronunciationAttempt.Analysis explain(AzurePronunciationAssessor.Result azure,
                                                  PronunciationAttempt.Transcript transcript) {
        requireConfigured();
        List<Candidate> candidates = candidates(azure.words());
        if (candidates.isEmpty()) {
            return PronunciationAttempt.Analysis.builder().provider("gemini").status("not_needed")
                    .summaryVi("Azure chưa đánh dấu lỗi phát âm nổi bật. Hãy duy trì nhịp đọc đều và đọc lại câu một lần.")
                    .correctedText("").grammarNoteVi("").interpretations(List.of()).build();
        }
        try {
            JsonNode response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                    .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody(candidates, transcript)).retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(JsonNode.class)
                            .defaultIfEmpty(objectMapper.createObjectNode())
                            .map(ignored -> new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                                    "Gemini chưa thể diễn giải lỗi phát âm.")))
                    .bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            return parse(response, candidates);
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Gemini chưa thể diễn giải lỗi phát âm.", exception);
        }
    }

    private ObjectNode requestBody(List<Candidate> candidates, PronunciationAttempt.Transcript transcript) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode parts = root.putArray("contents").addObject().putArray("parts");
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("whisperXTranscript", transcript == null ? "" : transcript.getText());
        ArrayNode issueList = evidence.putArray("azureIssues");
        candidates.forEach(candidate -> issueList.addObject().put("wordIndex", candidate.index())
                .put("referenceWord", candidate.text()).put("accuracyScore", candidate.score())
                .put("errorType", candidate.errorType()));
        parts.addObject().put("text", """
                You are a Korean pronunciation coach writing for a Vietnamese learner.
                Produce Vietnamese explanations ONLY for the supplied Azure issues. The Azure scores and error types are fixed evidence.
                Do not create, change, repeat, or rank scores. Do not claim to hear audio, identify a vowel/consonant/batchim, correct grammar, or rewrite the transcript.
                Return short, practical Vietnamese coaching only. Each item must reference one supplied wordIndex exactly once at most.
                Evidence JSON:
                %s
                """.formatted(evidence));
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", 0.1).put("responseMimeType", "application/json");
        config.set("responseJsonSchema", schema());
        return root;
    }

    private JsonNode schema() {
        try {
            return objectMapper.readTree("""
                    {"type":"object","properties":{"summaryVi":{"type":"string"},"items":{"type":"array","items":{"type":"object","properties":{"wordIndex":{"type":"integer"},"explanationVi":{"type":"string"},"practiceTipVi":{"type":"string"}},"required":["wordIndex","explanationVi","practiceTipVi"]}}},"required":["summaryVi","items"]}
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare Gemini interpretation schema", exception);
        }
    }

    private PronunciationAttempt.Analysis parse(JsonNode response, List<Candidate> candidates) {
        JsonNode text = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || !text.isTextual()) throw new IllegalStateException("Gemini returned no interpretation");
        try {
            JsonNode data = objectMapper.readTree(text.asText());
            String summary = data.path("summaryVi").asText("").trim();
            if (summary.isBlank() || !data.path("items").isArray()) throw new IllegalStateException("Gemini response is incomplete");
            Set<Integer> allowed = new HashSet<>();
            candidates.forEach(candidate -> allowed.add(candidate.index()));
            Set<Integer> seen = new HashSet<>();
            List<PronunciationAttempt.Interpretation> items = new ArrayList<>();
            for (JsonNode item : data.path("items")) {
                if (!item.path("wordIndex").canConvertToInt()) throw new IllegalStateException("Gemini returned invalid word index");
                int index = item.path("wordIndex").asInt();
                String explanation = item.path("explanationVi").asText("").trim();
                String tip = item.path("practiceTipVi").asText("").trim();
                if (!allowed.contains(index) || !seen.add(index) || explanation.isBlank() || tip.isBlank()) {
                    throw new IllegalStateException("Gemini returned unsupported interpretation");
                }
                items.add(PronunciationAttempt.Interpretation.builder().wordIndex(index)
                        .explanationVi(explanation).practiceTipVi(tip).build());
            }
            return PronunciationAttempt.Analysis.builder().provider("gemini").status("completed")
                    .summaryVi(summary).correctedText("").grammarNoteVi("").interpretations(items).build();
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini returned invalid interpretation JSON", exception);
        }
    }

    private List<Candidate> candidates(List<PronunciationAttempt.WordFeedback> words) {
        List<Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < words.size(); index++) {
            PronunciationAttempt.WordFeedback word = words.get(index);
            boolean error = word.getErrorType() != null && !"none".equalsIgnoreCase(word.getErrorType());
            if (error || word.getScore() != null && word.getScore() < 85) {
                candidates.add(new Candidate(index, word.getText(), word.getScore() == null ? 0 : word.getScore(), word.getErrorType()));
            }
        }
        return candidates.stream().limit(3).toList();
    }

    private void requireConfigured() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini chưa được cấu hình.");
        }
    }

    private record Candidate(int index, String text, int score, String errorType) { }
}

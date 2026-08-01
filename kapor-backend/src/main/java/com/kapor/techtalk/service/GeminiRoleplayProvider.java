package com.kapor.techtalk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.model.TechTalkScenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiRoleplayProvider implements RoleplayAiProvider {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final Set<String> CORRECTION_TYPES = Set.of(
            "particle", "honorific", "vocabulary", "grammar", "verb_ending", "pronoun", "formality");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    @Value("${techtalk.ai.temperature:0.65}")
    private double temperature;

    @Value("${techtalk.ai.max-output-tokens:512}")
    private int maxOutputTokens;

    @Override
    public Flux<String> streamReply(RoleplayContext context) {
        if (!configured()) return Flux.error(notConfigured());
        return webClientBuilder.build().post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("generativelanguage.googleapis.com")
                        .path("/v1beta/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .build(modelName))
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatRequest(context))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new RoleplayAiException(
                                "Gemini roleplay returned HTTP " + response.statusCode().value(),
                                response.statusCode().is5xxServerError() || response.statusCode().value() == 429)))
                .bodyToFlux(JsonNode.class)
                .map(this::candidateText)
                .filter(text -> !text.isEmpty())
                .timeout(REQUEST_TIMEOUT)
                .retryWhen(Retry.max(1).filter(this::retryable))
                .onErrorMap(error -> error instanceof RoleplayAiException
                        ? error
                        : new RoleplayAiException("Không thể nhận phản hồi TechTalk từ Gemini.", true, error));
    }

    @Override
    public Mono<RoleplaySession.Evaluation> evaluateTurn(RoleplayContext context, String userMessage) {
        if (!configured()) return Mono.error(notConfigured());
        String recentConversation = context.conversation().stream()
                .skip(Math.max(0, context.conversation().size() - 8L))
                .map(message -> message.getRole() + ": " + message.getContent())
                .reduce("", (left, right) -> left + "\n" + right);
        String prompt = """
                Evaluate the learner's latest Korean workplace message. Use the scenario, persona, objectives,
                required vocabulary, and the entire recent conversation supplied below. Scores are integers from
                0 to 100. Evaluate mission objectives cumulatively using evidence from the conversation, returning
                exactly one objective result for every scenario objective in the same order. An objective remains
                completed once the transcript contains sufficient evidence. Corrections must quote an exact
                substring of the learner message. Keep corrections concise and write noteVi and feedbackVi in
                Vietnamese. Do not penalize established English IT terms.

                If every objective is completed, completionMessageKo must be one natural, concise, in-character
                Korean sentence that acknowledges completion and closes the scenario. Otherwise it must be an empty
                string. The closing sentence must be plain text without Markdown, asterisks, headings, or bullets.

                Learner message:
                %s

                Scenario:
                %s

                Recent conversation:
                %s
                """.formatted(userMessage, compactScenario(context.scenario()), recentConversation);
        return generateStructured(prompt, turnEvaluationSchema())
                .map(node -> parseTurnEvaluation(node, userMessage))
                .onErrorMap(error -> error instanceof RoleplayAiException
                        ? error
                        : new RoleplayAiException("Gemini không thể đánh giá lượt hội thoại.", true, error));
    }

    @Override
    public Mono<RoleplaySession.FinalEvaluation> evaluateSession(RoleplayContext context) {
        if (!configured()) return Mono.error(notConfigured());
        String transcript = context.session().getMessages().stream()
                .map(message -> message.getRole() + ": " + message.getContent())
                .reduce("", (left, right) -> left + "\n" + right);
        String prompt = """
                Evaluate whether the learner completed the workplace mission objectives. Base the taskCompletion
                score only on evidence in the transcript. Return concise feedback in Korean and Vietnamese and
                2-4 actionable improvement areas in Vietnamese. Each objective result must quote brief evidence,
                or explain that no evidence was present.

                Scenario:
                %s

                Transcript:
                %s
                """.formatted(compactScenario(context.scenario()), transcript);
        return generateStructured(prompt, finalEvaluationSchema()).map(this::parseFinalEvaluation);
    }

    @Override
    public Mono<RoleplayHintDto> hint(RoleplayContext context) {
        if (!configured()) return Mono.error(notConfigured());
        String latest = context.conversation().stream().skip(Math.max(0, context.conversation().size() - 6L))
                .map(message -> message.getRole() + ": " + message.getContent())
                .reduce("", (left, right) -> left + "\n" + right);
        String prompt = """
                Give the learner one scaffold hint for the next Korean workplace response. Select at most five
                scenario-relevant Korean keywords, one reusable Korean sentence structure, and one short
                politeness tip in Vietnamese. Do not write the complete answer for the learner.

                Scenario:
                %s
                Recent conversation:
                %s
                """.formatted(compactScenario(context.scenario()), latest);
        return generateStructured(prompt, hintSchema()).map(this::parseHint);
    }

    @Override
    public String modelName() {
        return modelName;
    }

    private ObjectNode chatRequest(RoleplayContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", context.systemPrompt());
        ArrayNode contents = root.putArray("contents");
        for (RoleplaySession.Message message : context.conversation()) {
            if (message.getContent() == null || message.getContent().isBlank()) continue;
            String role = "ai".equals(message.getRole()) ? "model" : "user";
            contents.addObject().put("role", role).putArray("parts").addObject().put("text", message.getContent());
        }
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", Math.max(0, Math.min(1.5, temperature)));
        config.put("maxOutputTokens", Math.max(64, Math.min(1024, maxOutputTokens)));
        config.put("topP", 0.9);
        return root;
    }

    private Mono<JsonNode> generateStructured(String prompt, JsonNode schema) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("contents").addObject().put("role", "user")
                .putArray("parts").addObject().put("text", prompt);
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", 0.15);
        config.put("maxOutputTokens", 1024);
        config.put("responseMimeType", "application/json");
        config.set("responseJsonSchema", schema);
        return webClientBuilder.build().post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(root)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new RoleplayAiException(
                                "Gemini evaluation returned HTTP " + response.statusCode().value(),
                                response.statusCode().is5xxServerError() || response.statusCode().value() == 429)))
                .bodyToMono(JsonNode.class)
                .timeout(REQUEST_TIMEOUT)
                .retryWhen(Retry.max(1).filter(this::retryable))
                .map(this::structuredCandidate);
    }

    private JsonNode structuredCandidate(JsonNode response) {
        String text = candidateText(response);
        if (text.isBlank()) throw new RoleplayAiException("Gemini returned no structured roleplay result.", true);
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException exception) {
            throw new RoleplayAiException("Gemini returned malformed roleplay JSON.", true, exception);
        }
    }

    private String candidateText(JsonNode response) {
        if (response == null) return "";
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) return "";
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("text").isTextual()) text.append(part.path("text").asText());
        }
        return text.toString();
    }

    private RoleplaySession.Evaluation parseTurnEvaluation(JsonNode node, String source) {
        List<RoleplaySession.Correction> corrections = new ArrayList<>();
        for (JsonNode correction : node.path("corrections")) {
            String original = correction.path("original").asText("").trim();
            String suggestion = correction.path("suggestion").asText("").trim();
            if (original.isBlank() || suggestion.isBlank() || !source.contains(original)) continue;
            String type = correction.path("type").asText("grammar").toLowerCase(Locale.ROOT);
            corrections.add(RoleplaySession.Correction.builder()
                    .original(original)
                    .suggestion(suggestion)
                    .type(CORRECTION_TYPES.contains(type) ? type : "grammar")
                    .noteVi(correction.path("noteVi").asText("Điều chỉnh cho phù hợp ngữ cảnh công việc."))
                    .build());
        }
        List<String> usedVocabulary = new ArrayList<>();
        node.path("usedRequiredVocabulary").forEach(item -> {
            if (item.isTextual() && source.contains(item.asText())) usedVocabulary.add(item.asText());
        });
        List<RoleplaySession.ObjectiveResult> objectives = new ArrayList<>();
        for (JsonNode item : node.path("objectives")) {
            objectives.add(RoleplaySession.ObjectiveResult.builder()
                    .objective(item.path("objective").asText("").trim())
                    .completed(item.path("completed").asBoolean(false))
                    .evidence(item.path("evidence").asText("").trim())
                    .build());
        }
        return RoleplaySession.Evaluation.builder()
                .grammar(score(node.path("grammar").asInt()))
                .vocabulary(score(node.path("vocabulary").asInt()))
                .politeness(score(node.path("politeness").asInt()))
                .status("completed")
                .feedbackVi(node.path("feedbackVi").asText("").trim())
                .corrections(corrections)
                .usedRequiredVocabulary(usedVocabulary)
                .objectives(objectives)
                .completionMessageKo(node.path("completionMessageKo").asText("").trim())
                .build();
    }

    private RoleplaySession.FinalEvaluation parseFinalEvaluation(JsonNode node) {
        List<RoleplaySession.ObjectiveResult> objectives = new ArrayList<>();
        for (JsonNode item : node.path("objectives")) {
            objectives.add(RoleplaySession.ObjectiveResult.builder()
                    .objective(item.path("objective").asText(""))
                    .completed(item.path("completed").asBoolean(false))
                    .evidence(item.path("evidence").asText(""))
                    .build());
        }
        List<String> improvements = new ArrayList<>();
        node.path("improvementAreas").forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) improvements.add(item.asText().trim());
        });
        return RoleplaySession.FinalEvaluation.builder()
                .taskCompletion(score(node.path("taskCompletion").asInt()))
                .feedback(node.path("feedback").asText("").trim())
                .feedbackVi(node.path("feedbackVi").asText("").trim())
                .improvementAreas(improvements)
                .objectives(objectives)
                .build();
    }

    private RoleplayHintDto parseHint(JsonNode node) {
        List<String> keywords = new ArrayList<>();
        node.path("keywords").forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank() && keywords.size() < 5) keywords.add(item.asText().trim());
        });
        return RoleplayHintDto.builder()
                .keywords(keywords)
                .sentenceStructure(node.path("sentenceStructure").asText("").trim())
                .politenessTip(node.path("politenessTip").asText("").trim())
                .build();
    }

    private String compactScenario(TechTalkScenario scenario) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("title", scenario.getTitle());
            node.put("difficulty", scenario.getDifficulty());
            node.set("persona", objectMapper.valueToTree(scenario.getPersona()));
            node.set("mission", objectMapper.valueToTree(scenario.getMission()));
            node.set("objectives", objectMapper.valueToTree(scenario.getObjectives()));
            node.set("requiredVocabulary", objectMapper.valueToTree(scenario.getRequiredVocabulary()));
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new RoleplayAiException("Unable to prepare roleplay scenario.", false, exception);
        }
    }

    private JsonNode turnEvaluationSchema() {
        return jsonSchema("""
                {"type":"object","properties":{
                  "grammar":{"type":"integer","minimum":0,"maximum":100},
                  "vocabulary":{"type":"integer","minimum":0,"maximum":100},
                  "politeness":{"type":"integer","minimum":0,"maximum":100},
                  "feedbackVi":{"type":"string"},
                  "usedRequiredVocabulary":{"type":"array","items":{"type":"string"}},
                  "completionMessageKo":{"type":"string"},
                  "objectives":{"type":"array","items":{"type":"object","properties":{
                    "objective":{"type":"string"},"completed":{"type":"boolean"},"evidence":{"type":"string"}
                  },"required":["objective","completed","evidence"]}},
                  "corrections":{"type":"array","items":{"type":"object","properties":{
                    "original":{"type":"string"},"suggestion":{"type":"string"},
                    "type":{"type":"string","enum":["particle","honorific","vocabulary","grammar","verb_ending","pronoun","formality"]},
                    "noteVi":{"type":"string"}
                  },"required":["original","suggestion","type","noteVi"]}}
                },"required":["grammar","vocabulary","politeness","feedbackVi","usedRequiredVocabulary",
                  "objectives","completionMessageKo","corrections"]}
                """);
    }

    private JsonNode finalEvaluationSchema() {
        return jsonSchema("""
                {"type":"object","properties":{
                  "taskCompletion":{"type":"integer","minimum":0,"maximum":100},
                  "feedback":{"type":"string"},"feedbackVi":{"type":"string"},
                  "improvementAreas":{"type":"array","items":{"type":"string"}},
                  "objectives":{"type":"array","items":{"type":"object","properties":{
                    "objective":{"type":"string"},"completed":{"type":"boolean"},"evidence":{"type":"string"}
                  },"required":["objective","completed","evidence"]}}
                },"required":["taskCompletion","feedback","feedbackVi","improvementAreas","objectives"]}
                """);
    }

    private JsonNode hintSchema() {
        return jsonSchema("""
                {"type":"object","properties":{
                  "keywords":{"type":"array","items":{"type":"string"}},
                  "sentenceStructure":{"type":"string"},"politenessTip":{"type":"string"}
                },"required":["keywords","sentenceStructure","politenessTip"]}
                """);
    }

    private JsonNode jsonSchema(String source) {
        try {
            return objectMapper.readTree(source);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid bundled TechTalk JSON schema", exception);
        }
    }

    private int score(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private boolean configured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your-");
    }

    private RoleplayAiException notConfigured() {
        return new RoleplayAiException("TechTalk AI chưa được cấu hình. Hãy thiết lập GEMINI_API_KEY.", false);
    }

    private boolean retryable(Throwable error) {
        return !(error instanceof RoleplayAiException exception) || exception.isRetryable();
    }
}

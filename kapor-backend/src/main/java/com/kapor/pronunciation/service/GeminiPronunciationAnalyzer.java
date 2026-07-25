package com.kapor.pronunciation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;
import java.util.Locale;

/**
 * Gemini is deliberately given text-only evidence: the Korean reference, the
 * Whisper transcript, and recording duration. It must not claim it heard a
 * precise phoneme error from a transcript alone.
 */
@Service
public class GeminiPronunciationAnalyzer {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    public GeminiPronunciationAnalyzer(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Result analyze(String referenceText, String transcription, double durationSeconds) {
        requireConfigured();
        try {
            JsonNode response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody(referenceText, transcription, durationSeconds))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(JsonNode.class)
                            .defaultIfEmpty(objectMapper.createObjectNode())
                            .map(body -> geminiError(clientResponse.statusCode(), body)))
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
            return parse(response, referenceText);
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Gemini chưa thể phân tích kết quả phát âm. Vui lòng thử lại.", exception);
        }
    }

    private void requireConfigured() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini chưa được cấu hình. Hãy đặt GEMINI_API_KEY trên máy chủ.");
        }
    }

    private ObjectNode requestBody(String referenceText, String transcription, double durationSeconds) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode parts = root.putArray("contents").addObject().putArray("parts");
        parts.addObject().put("text", prompt(referenceText, transcription, durationSeconds));

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseJsonSchema", responseSchema());
        return root;
    }

    private String prompt(String referenceText, String transcription, double durationSeconds) {
        try {
            return """
                    You are a careful Korean language coach for a Vietnamese learner.
                    Assess a learner reading a fixed Korean reference sentence using ONLY the reference text, Whisper transcript, and duration below.
                    The transcript is text evidence, not a phoneme-level acoustic measurement. Never claim to hear a particular vowel, consonant, 받침, intonation, or sound error. Explain discrepancies as "Whisper transcribed ..." or as a text mismatch.

                    Return Vietnamese learner-facing feedback. Score every field from 0 to 100 as an integer:
                    - accuracy: how closely the transcript matches the reference sentence.
                    - completeness: whether all reference words were transcribed.
                    - fluency: a conservative estimate from the transcript and duration only; do not claim acoustic evidence.
                    - overall: a balanced score; do not inflate a mismatched transcript.
                    - correctedText: the grammatical correction of the transcript. If it is already a natural reading of the reference, return the reference exactly.
                    - grammarNoteVi: a concise note explaining the most useful grammar or wording correction. Return an empty string if none is needed.
                    - summaryVi: concise, actionable feedback in Vietnamese.
                    - wordFeedback: exactly one object, in the same order, for every whitespace-delimited word in referenceText. Its text must be copied exactly from that reference word. Use phonemeDetail for a short Vietnamese text-comparison explanation, never an acoustic or phoneme claim. accuracy must be one of good, needs_practice, retry.

                    Input JSON:
                    %s
                    """.formatted(objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("referenceText", referenceText == null ? "" : referenceText.trim())
                    .put("whisperTranscript", transcription == null ? "" : transcription.trim())
                    .put("durationSeconds", Math.round(Math.max(0d, durationSeconds) * 100d) / 100d)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare Gemini pronunciation prompt", exception);
        }
    }

    private JsonNode responseSchema() {
        try {
            return objectMapper.readTree("""
                    {"type":"object","properties":{
                      "accuracy":{"type":"integer"},"fluency":{"type":"integer"},"completeness":{"type":"integer"},"overall":{"type":"integer"},
                      "summaryVi":{"type":"string"},"correctedText":{"type":"string"},"grammarNoteVi":{"type":"string"},
                      "wordFeedback":{"type":"array","items":{"type":"object","properties":{
                        "text":{"type":"string"},"score":{"type":"integer"},"accuracy":{"type":"string","enum":["good","needs_practice","retry"]},"phonemeDetail":{"type":"string"}
                      },"required":["text","score","accuracy","phonemeDetail"]}}
                    },"required":["accuracy","fluency","completeness","overall","summaryVi","correctedText","grammarNoteVi","wordFeedback"]}
                    """);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare Gemini pronunciation schema", exception);
        }
    }

    private Result parse(JsonNode response, String referenceText) {
        if (response == null) throw new IllegalStateException("Gemini returned an empty response");
        JsonNode text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isTextual()) throw new IllegalStateException("Gemini did not return pronunciation analysis");
        try {
            JsonNode data = objectMapper.readTree(text.asText());
            List<String> referenceWords = words(referenceText);
            JsonNode feedbackNode = data.path("wordFeedback");
            if (!feedbackNode.isArray() || feedbackNode.size() != referenceWords.size()) {
                throw new IllegalStateException("Gemini returned incomplete word feedback");
            }
            List<PronunciationAttempt.WordFeedback> feedback = new ArrayList<>();
            for (int index = 0; index < referenceWords.size(); index++) {
                JsonNode word = feedbackNode.get(index);
                String textValue = word.path("text").asText("").trim();
                if (!referenceWords.get(index).equals(textValue)) {
                    throw new IllegalStateException("Gemini returned word feedback out of order");
                }
                int score = score(word.path("score"));
                feedback.add(PronunciationAttempt.WordFeedback.builder()
                        .text(textValue).score(score).accuracy(label(word.path("accuracy").asText(), score))
                        .phonemeDetail(word.path("phonemeDetail").asText("").trim()).build());
            }
            PronunciationAttempt.Scores scores = PronunciationAttempt.Scores.builder()
                    .accuracy(score(data.path("accuracy"))).fluency(score(data.path("fluency")))
                    .completeness(score(data.path("completeness"))).overall(score(data.path("overall"))).build();
            PronunciationAttempt.Analysis analysis = PronunciationAttempt.Analysis.builder()
                    .summaryVi(requiredText(data, "summaryVi"))
                    .correctedText(requiredText(data, "correctedText"))
                    .grammarNoteVi(data.path("grammarNoteVi").asText("").trim()).build();
            return new Result(scores, feedback, analysis);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gemini returned invalid pronunciation JSON", exception);
        }
    }

    private List<String> words(String text) {
        String value = text == null ? "" : text.trim();
        return value.isBlank() ? List.of() : List.of(value.split("\\s+"));
    }

    private int score(JsonNode value) {
        if (!value.canConvertToInt()) throw new IllegalStateException("Gemini returned an invalid score");
        return Math.max(0, Math.min(100, value.asInt()));
    }

    private String requiredText(JsonNode data, String field) {
        String value = data.path(field).asText("").trim();
        if (value.isBlank()) throw new IllegalStateException("Gemini returned an empty " + field);
        return value;
    }

    private String label(String value, int score) {
        return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
            case "good", "needs_practice", "retry" -> value.toLowerCase(Locale.ROOT);
            default -> score >= 85 ? "good" : score >= 60 ? "needs_practice" : "retry";
        };
    }

    private PronunciationAssessmentException geminiError(HttpStatusCode status, JsonNode body) {
        if (status.value() == 401 || status.value() == 403) {
            return new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Gemini không chấp nhận API key của máy chủ.");
        }
        if (status.value() == 404) {
            return new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Gemini model '" + modelName + "' không khả dụng cho API key này.");
        }
        if (status.value() == 429) {
            return new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini đang đạt giới hạn. Hãy thử lại sau ít phút.");
        }
        String detail = body.path("error").path("message").asText("").replaceAll("[\\r\\n]+", " ").trim();
        return new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                detail.isBlank() ? "Gemini không thể phân tích kết quả phát âm." : "Gemini từ chối yêu cầu phân tích.");
    }

    public record Result(PronunciationAttempt.Scores scores,
                         List<PronunciationAttempt.WordFeedback> wordFeedback,
                         PronunciationAttempt.Analysis analysis) { }
}

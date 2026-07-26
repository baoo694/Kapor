package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.pronunciation.exception.PronunciationAssessmentException;
import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Server-side Azure Speech Pronunciation Assessment adapter. This is the only
 * component allowed to produce pronunciation scores. It intentionally uses
 * Azure's detailed REST result instead of its recognized text as the product
 * transcript; WhisperX owns that separate responsibility.
 */
@Service
@RequiredArgsConstructor
public class AzurePronunciationAssessor {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    /**
     * Azure requires the PCM metadata for pronunciation assessment. The codec
     * value contains a slash, so it must be quoted to satisfy Spring's HTTP
     * media type parser before the request body is written.
     */
    static final MediaType AZURE_WAV_CONTENT_TYPE =
            MediaType.parseMediaType("audio/wav; codecs=\"audio/pcm\"; samplerate=16000");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${azure.speech.key:}")
    private String speechKey;
    @Value("${azure.speech.region:}")
    private String region;
    @Value("${azure.speech.locale:ko-KR}")
    private String locale;

    public Result assess(String referenceText, byte[] wavAudio) {
        requireConfigured();
        if (referenceText == null || referenceText.isBlank()) {
            throw new IllegalArgumentException("Câu mẫu để đánh giá phát âm không được để trống.");
        }
        try {
            JsonNode response = webClientBuilder.build().post().uri(endpoint())
                    .header("Ocp-Apim-Subscription-Key", speechKey.trim())
                    .header("Pronunciation-Assessment", assessmentHeader(referenceText))
                    .contentType(AZURE_WAV_CONTENT_TYPE).bodyValue(wavAudio).retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("").map(ignored -> azureError(clientResponse.statusCode())))
                    .bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            return parse(response);
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Azure chưa thể đánh giá phát âm. Vui lòng thử lại.", exception);
        }
    }

    /** Package-visible for fixture-based parsing tests; never persists raw Azure JSON. */
    Result parse(JsonNode response) {
        if (response == null || !"Success".equalsIgnoreCase(response.path("RecognitionStatus").asText())) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Azure không nhận được câu nói hợp lệ để đánh giá.");
        }
        JsonNode best = response.path("NBest").path(0);
        JsonNode assessment = scoreNode(best);
        if (best.isMissingNode() || !assessment.path("AccuracyScore").isNumber()) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Azure trả về kết quả đánh giá không đầy đủ.");
        }

        int accuracy = requiredScore(assessment, "AccuracyScore");
        int fluency = requiredScore(assessment, "FluencyScore");
        int completeness = requiredScore(assessment, "CompletenessScore");
        int pronunciation = score(assessment, "PronScore", accuracy);
        PronunciationAttempt.Scores scores = PronunciationAttempt.Scores.builder()
                .accuracy(accuracy).fluency(fluency).completeness(completeness)
                .pronunciation(pronunciation).overall(pronunciation).build();
        return new Result(scores, words(best.path("Words")));
    }

    private List<PronunciationAttempt.WordFeedback> words(JsonNode source) {
        if (!source.isArray()) return List.of();
        List<PronunciationAttempt.WordFeedback> words = new ArrayList<>();
        for (JsonNode word : source) {
            JsonNode assessment = scoreNode(word);
            int score = score(assessment, "AccuracyScore", 0);
            String errorType = assessment.path("ErrorType").asText("None");
            words.add(PronunciationAttempt.WordFeedback.builder()
                    .text(word.path("Word").asText(""))
                    .score(score).accuracy(label(score, errorType)).errorType(errorType)
                    .offsetMs(ticksToMs(word.path("Offset"))).durationMs(ticksToMs(word.path("Duration")))
                    .phonemeDetail("").phonemes(phonemes(word.path("Phonemes"))).build());
        }
        return words;
    }

    private List<PronunciationAttempt.PhonemeFeedback> phonemes(JsonNode source) {
        if (!source.isArray()) return List.of();
        List<PronunciationAttempt.PhonemeFeedback> phonemes = new ArrayList<>();
        int index = 0;
        for (JsonNode phoneme : source) {
            JsonNode assessment = scoreNode(phoneme);
            // Azure does not expose phoneme names for ko-KR. Preserve a name only
            // when the service explicitly sends one; never derive one with an LLM.
            String name = phoneme.path("Phoneme").isTextual() ? phoneme.path("Phoneme").asText() : null;
            phonemes.add(PronunciationAttempt.PhonemeFeedback.builder().index(index++)
                    .score(score(assessment, "AccuracyScore", score(phoneme, "Score", 0))).phoneme(name).build());
        }
        return phonemes;
    }

    /**
     * Azure returns both schemas in the wild: the original nested
     * PronunciationAssessment object and the flat score fields used by the
     * current Korean REST response. Both values are Azure evidence.
     */
    private JsonNode scoreNode(JsonNode source) {
        JsonNode nested = source.path("PronunciationAssessment");
        return nested.isObject() ? nested : source;
    }

    private String assessmentHeader(String referenceText) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("ReferenceText", referenceText.trim());
        value.put("GradingSystem", "HundredMark");
        value.put("Granularity", "Phoneme");
        value.put("Dimension", "Comprehensive");
        value.put("EnableMiscue", true);
        try {
            return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare Azure pronunciation request", exception);
        }
    }

    private String endpoint() {
        return "https://" + region.trim() + ".stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1"
                + "?language=" + locale.trim() + "&format=detailed";
    }

    private void requireConfigured() {
        if (speechKey == null || speechKey.isBlank() || speechKey.startsWith("your-")
                || region == null || region.isBlank() || region.startsWith("your-")) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Azure Speech chưa được cấu hình. Hãy đặt AZURE_SPEECH_KEY và AZURE_SPEECH_REGION.");
        }
    }

    private PronunciationAssessmentException azureError(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) {
            return new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Azure Speech không chấp nhận thông tin xác thực của máy chủ.");
        }
        if (status.value() == 429) {
            return new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Azure Speech đang đạt giới hạn. Vui lòng thử lại sau ít phút.");
        }
        return new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                "Azure Speech không thể đánh giá bản ghi này.");
    }

    private int requiredScore(JsonNode source, String key) {
        if (!source.path(key).isNumber()) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "Azure trả về điểm đánh giá không đầy đủ.");
        }
        return score(source, key, 0);
    }

    private int score(JsonNode source, String key, int fallback) {
        JsonNode value = source.path(key);
        if (!value.isNumber()) return fallback;
        return Math.max(0, Math.min(100, (int) Math.round(value.asDouble())));
    }

    private Long ticksToMs(JsonNode ticks) {
        return ticks.isNumber() ? Math.max(0, Math.round(ticks.asDouble() / 10_000d)) : null;
    }

    private String label(int score, String errorType) {
        if (errorType != null && !"none".equalsIgnoreCase(errorType)) return "needs_practice";
        return score >= 85 ? "good" : score >= 60 ? "needs_practice" : "retry";
    }

    public record Result(PronunciationAttempt.Scores scores, List<PronunciationAttempt.WordFeedback> words) { }
}

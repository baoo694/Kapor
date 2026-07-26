package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kapor.pronunciation.exception.PronunciationAssessmentException;
import com.kapor.pronunciation.model.PronunciationAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** WhisperX is the sole source of learner transcript and UI timestamps. */
@Service
@RequiredArgsConstructor
public class WhisperXTranscriber {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final WebClient.Builder webClientBuilder;
    @Value("${whisperx.service-url:${whisper.service-url:http://localhost:8001}}")
    private String serviceUrl;

    public PronunciationAttempt.Transcript transcribe(byte[] wavAudio, String referenceText) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("audio", new WavResource(wavAudio)).filename("attempt.wav").contentType(MediaType.valueOf("audio/wav"));
        // This is an ASR decoding hint for the known scripted exercise, never a
        // replacement transcript. WhisperX can still return what it hears.
        body.part("referenceText", referenceText == null ? "" : referenceText.trim())
                .contentType(MediaType.TEXT_PLAIN);
        try {
            JsonNode response = webClientBuilder.build().post().uri(normalizedServiceUrl() + "/pronunciation/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA).bodyValue(body.build())
                    .retrieve().bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            if (response == null || !response.path("text").isTextual()) {
                throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                        "WhisperX trả về transcript không hợp lệ.");
            }
            return PronunciationAttempt.Transcript.builder().provider("whisperx")
                    .text(response.path("text").asText("").trim())
                    .durationSeconds(response.path("durationSeconds").asDouble(0d))
                    .words(words(response.path("words"))).build();
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WhisperX chưa sẵn sàng. Hãy kiểm tra dịch vụ NLP và model căn chỉnh tiếng Hàn.", exception);
        }
    }

    private List<PronunciationAttempt.TranscriptWord> words(JsonNode source) {
        if (!source.isArray()) return List.of();
        List<PronunciationAttempt.TranscriptWord> words = new ArrayList<>();
        for (JsonNode item : source) {
            boolean aligned = item.path("startSeconds").isNumber() && item.path("endSeconds").isNumber();
            words.add(PronunciationAttempt.TranscriptWord.builder().text(item.path("text").asText("").trim())
                    .startSeconds(aligned ? item.path("startSeconds").asDouble() : null)
                    .endSeconds(aligned ? item.path("endSeconds").asDouble() : null)
                    .confidence(item.path("probability").isNumber() ? item.path("probability").asDouble() : null)
                    .timingStatus(item.path("timingStatus").asText(aligned ? "aligned" : "unaligned")).build());
        }
        return words;
    }

    private String normalizedServiceUrl() {
        return serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
    }

    private static final class WavResource extends ByteArrayResource {
        private WavResource(byte[] bytes) { super(bytes); }
        @Override public String getFilename() { return "attempt.wav"; }
    }
}

package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kapor.pronunciation.exception.PronunciationAssessmentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Locale;

/**
 * Whisper transcribes the recording; Gemini owns the learner-facing analysis.
 * The selected Whisper provider can be the local service or OpenAI's hosted
 * Whisper API, configured without changing the mobile client.
 */
@Service
public class LocalWhisperPronunciationAssessmentProvider implements PronunciationAssessmentProvider {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private final WebClient webClient;
    private final GeminiPronunciationAnalyzer geminiAnalyzer;

    @Value("${whisper.provider:local}")
    private String provider;
    @Value("${whisper.service-url:http://localhost:8001}")
    private String serviceUrl;
    @Value("${openai.api-key:}")
    private String openAiApiKey;
    @Value("${openai.whisper.model-name:whisper-1}")
    private String openAiModelName;

    public LocalWhisperPronunciationAssessmentProvider(
            WebClient.Builder webClientBuilder, GeminiPronunciationAnalyzer geminiAnalyzer) {
        this.webClient = webClientBuilder.build();
        this.geminiAnalyzer = geminiAnalyzer;
    }

    @Override
    public String name() {
        return "openai".equals(selectedProvider()) ? "openai_whisper_gemini" : "local_whisper_gemini";
    }

    @Override
    public Assessment assess(String userId, String referenceText, byte[] wavAudio) {
        Transcription transcription = transcribe(wavAudio);
        GeminiPronunciationAnalyzer.Result analysis = geminiAnalyzer.analyze(
                referenceText, transcription.text(), transcription.durationSeconds());
        return new Assessment(analysis.scores(), transcription.text(), analysis.wordFeedback(), analysis.analysis());
    }

    private Transcription transcribe(byte[] wavAudio) {
        return switch (selectedProvider()) {
            case "local" -> transcribeLocal(wavAudio);
            case "openai" -> transcribeOpenAi(wavAudio);
            default -> throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WHISPER_PROVIDER phải là 'local' hoặc 'openai'.");
        };
    }

    private Transcription transcribeLocal(byte[] wavAudio) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("audio", new WavResource(wavAudio)).filename("attempt.wav").contentType(MediaType.valueOf("audio/wav"));
        try {
            JsonNode response = webClient.post().uri(normalizedServiceUrl() + "/pronunciation/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA).bodyValue(body.build()).retrieve()
                    .bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            return transcription(response, wavAudio, "Whisper local");
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Whisper local chưa sẵn sàng. Hãy khởi động kapor-nlp và chờ model tải xong.", exception);
        }
    }

    private Transcription transcribeOpenAi(byte[] wavAudio) {
        if (openAiApiKey == null || openAiApiKey.isBlank() || openAiApiKey.startsWith("your-")) {
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI Whisper chưa được cấu hình. Hãy đặt OPENAI_API_KEY và WHISPER_PROVIDER=openai.");
        }
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new WavResource(wavAudio)).filename("attempt.wav").contentType(MediaType.valueOf("audio/wav"));
        body.part("model", openAiModelName);
        body.part("language", "ko");
        body.part("response_format", "verbose_json");
        try {
            JsonNode response = webClient.post().uri("https://api.openai.com/v1/audio/transcriptions")
                    .headers(headers -> headers.setBearerAuth(openAiApiKey))
                    .contentType(MediaType.MULTIPART_FORM_DATA).bodyValue(body.build()).retrieve()
                    .bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            return transcription(response, wavAudio, "OpenAI Whisper");
        } catch (PronunciationAssessmentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY,
                    "OpenAI Whisper chưa thể nhận diện bản ghi. Hãy kiểm tra API key và kết nối mạng.", exception);
        }
    }

    private Transcription transcription(JsonNode response, byte[] wavAudio, String source) {
        if (response == null || !response.path("text").isTextual()) {
            throw new PronunciationAssessmentException(HttpStatus.BAD_GATEWAY, source + " trả về transcript không hợp lệ.");
        }
        return new Transcription(response.path("text").asText().trim(),
                response.path("duration").asDouble(response.path("durationSeconds").asDouble(wavDurationSeconds(wavAudio))));
    }

    private String selectedProvider() {
        return provider == null ? "local" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedServiceUrl() {
        return serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
    }

    private double wavDurationSeconds(byte[] wav) {
        return Math.max(0, wav.length - 44) / 32_000d;
    }

    private static final class WavResource extends ByteArrayResource {
        private WavResource(byte[] bytes) { super(bytes); }
        @Override public String getFilename() { return "attempt.wav"; }
    }

    private record Transcription(String text, double durationSeconds) { }
}

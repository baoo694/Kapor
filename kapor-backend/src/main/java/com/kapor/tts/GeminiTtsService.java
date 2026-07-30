package com.kapor.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.video.exception.GeminiApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GeminiTtsService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Pattern SAMPLE_RATE = Pattern.compile("(?:^|;)\\s*rate=(\\d+)", Pattern.CASE_INSENSITIVE);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TtsAudioCache audioCache;
    private final TtsRateLimiter rateLimiter;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.tts.model-name:gemini-2.5-flash-preview-tts}")
    private String modelName;

    @Value("${gemini.tts.voice-name:Kore}")
    private String voiceName;

    @Value("${gemini.tts.prompt-version:v1}")
    private String promptVersion;

    public GeminiTtsService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            TtsAudioCache audioCache,
            TtsRateLimiter rateLimiter) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.audioCache = audioCache;
        this.rateLimiter = rateLimiter;
    }

    public byte[] synthesizeKorean(String userId, String rawText) {
        return synthesize(userId, rawText, "vocabulary", 180);
    }

    public byte[] synthesizeKoreanDialogue(String userId, String rawText) {
        return synthesize(userId, rawText, "dialogue", 800);
    }

    private byte[] synthesize(String userId, String rawText, String mode, int maxLength) {
        requireConfigured();
        String text = normalizeKoreanText(rawText, maxLength);
        String cacheKey = cacheKey(mode, text);

        Optional<byte[]> cachedAudio = audioCache.get(cacheKey);
        if (cachedAudio.isPresent()) return cachedAudio.get();

        if (!rateLimiter.tryAcquire(userId)) {
            throw new GeminiApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã yêu cầu phát âm quá nhiều từ mới. Vui lòng thử lại sau một phút.");
        }

        byte[] audio = requestAudioWithRetry(text, mode);
        audioCache.put(cacheKey, audio);
        return audio;
    }

    private void requireConfigured() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new GeminiApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini TTS chưa được cấu hình trên máy chủ.");
        }
    }

    private String normalizeKoreanText(String rawText, int maxLength) {
        String text = Normalizer.normalize(rawText == null ? "" : rawText, Normalizer.Form.NFC)
                .trim()
                .replaceAll("\\s+", " ");
        if (text.isEmpty()) throw new IllegalArgumentException("Từ tiếng Hàn không được để trống");
        if (text.length() > maxLength) {
            throw new IllegalArgumentException("Nội dung tiếng Hàn không được vượt quá " + maxLength + " ký tự");
        }
        if (text.chars().noneMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ phát âm từ có ký tự tiếng Hàn");
        }
        return text;
    }

    private byte[] requestAudioWithRetry(String text, String mode) {
        IllegalStateException malformedResponse = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return requestAudio(text, mode);
            } catch (IllegalStateException exception) {
                malformedResponse = exception;
                log.warn("Gemini TTS returned unusable audio on attempt {}", attempt + 1);
            }
        }
        throw new GeminiApiException(
                HttpStatus.BAD_GATEWAY,
                "Gemini TTS không trả về audio hợp lệ. Vui lòng thử lại.",
                malformedResponse);
    }

    private byte[] requestAudio(String text, String mode) {
        JsonNode response = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody(text, mode))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> geminiError(clientResponse.statusCode(), body)))
                .bodyToMono(JsonNode.class)
                .block(REQUEST_TIMEOUT);
        return parseAudioResponse(response);
    }

    private JsonNode requestBody(String text, String mode) {
        var root = objectMapper.createObjectNode();
        var parts = root.putArray("contents").addObject().putArray("parts");
        String instruction = "dialogue".equals(mode)
                ? """
                  Read the Korean workplace dialogue below naturally exactly once. Use a professional Seoul Korean
                  voice, preserve the intended politeness and sentence rhythm, and do not read any labels or instructions.
                  Dialogue: %s
                  """.formatted(text)
                : """
                  Read the Korean vocabulary item below exactly once. Use a clear, natural Seoul Korean pronunciation
                  at a learner-friendly pace. Do not read instructions, labels, translations, romanization, or punctuation.
                  Vocabulary item: %s
                  """.formatted(text);
        parts.addObject().put("text", instruction);

        var generationConfig = root.putObject("generationConfig");
        generationConfig.putArray("responseModalities").add("AUDIO");
        generationConfig.putObject("speechConfig")
                .putObject("voiceConfig")
                .putObject("prebuiltVoiceConfig")
                .put("voiceName", voiceName);
        return root;
    }

    private byte[] parseAudioResponse(JsonNode response) {
        if (response == null) throw new IllegalStateException("Gemini returned an empty response");
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) throw new IllegalStateException("Gemini returned no audio parts");

        for (JsonNode part : parts) {
            JsonNode inlineData = part.path("inlineData");
            if (!inlineData.path("data").isTextual()) continue;
            String mimeType = inlineData.path("mimeType").asText("");
            if (!mimeType.toLowerCase(Locale.ROOT).startsWith("audio/l16")) {
                throw new IllegalStateException("Gemini returned an unsupported audio format");
            }
            byte[] pcm = Base64.getDecoder().decode(inlineData.path("data").asText());
            if (pcm.length == 0) throw new IllegalStateException("Gemini returned empty audio");
            return wavFromPcm(pcm, sampleRate(mimeType));
        }
        throw new IllegalStateException("Gemini returned no audio data");
    }

    private int sampleRate(String mimeType) {
        Matcher matcher = SAMPLE_RATE.matcher(mimeType);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 24000;
    }

    static byte[] wavFromPcm(byte[] pcm, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(StandardCharsets.US_ASCII.encode("RIFF"));
        header.putInt(36 + pcm.length);
        header.put(StandardCharsets.US_ASCII.encode("WAVEfmt "));
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put(StandardCharsets.US_ASCII.encode("data"));
        header.putInt(pcm.length);

        byte[] wav = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }

    private String cacheKey(String mode, String text) {
        String source = String.join("|", promptVersion, modelName, voiceName, mode, text);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return "tts/korean/" + HexFormat.of().formatHex(digest) + ".wav";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private GeminiApiException geminiError(HttpStatusCode status, String body) {
        if (status.value() == 401 || status.value() == 403) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini TTS không chấp nhận API key của máy chủ.");
        }
        if (status.value() == 404) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "Gemini TTS model không khả dụng cho API key này.");
        }
        if (status.value() == 429) {
            return new GeminiApiException(HttpStatus.TOO_MANY_REQUESTS, "Gemini TTS đang đạt giới hạn. Vui lòng thử lại sau.");
        }
        return new GeminiApiException(
                HttpStatus.BAD_GATEWAY,
                body.isBlank() ? "Gemini TTS không thể tạo audio." : "Gemini TTS từ chối yêu cầu phát âm.");
    }
}

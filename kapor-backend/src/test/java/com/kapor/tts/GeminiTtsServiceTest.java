package com.kapor.tts;

import com.kapor.video.exception.GeminiApiException;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiTtsServiceTest {

    @Test
    void convertsUnexpectedGeminiTransportFailureToApiFailure() {
        TtsAudioCache audioCache = new TtsAudioCache(
                MinioClient.builder()
                        .endpoint("http://localhost:9000")
                        .credentials("minioadmin", "minioadmin")
                        .build(),
                "kapor-test") {
            @Override
            public Optional<byte[]> get(String objectName) {
                return Optional.empty();
            }

            @Override
            public void put(String objectName, byte[] audio) {
                // The transport failure happens before a cache write.
            }
        };
        WebClient.Builder failingClient = WebClient.builder().exchangeFunction(request -> Mono.error(
                new WebClientRequestException(
                        new IOException("offline"),
                        HttpMethod.POST,
                        URI.create("https://generativelanguage.googleapis.com"),
                        new HttpHeaders())));
        GeminiTtsService service = new GeminiTtsService(
                failingClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                audioCache,
                new TtsRateLimiter(12));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        GeminiApiException exception = assertThrows(
                GeminiApiException.class,
                () -> service.synthesizeKoreanDialogue("learner", "안녕하세요."));

        assertEquals(502, exception.getStatus().value());
    }

    @Test
    void wrapsGeminiPcmAudioAsAValidMonoWavFile() {
        byte[] pcm = {1, 2, 3, 4};

        byte[] wav = GeminiTtsService.wavFromPcm(pcm, 24000);
        ByteBuffer header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);

        byte[] riff = new byte[4];
        header.get(riff);
        assertArrayEquals("RIFF".getBytes(StandardCharsets.US_ASCII), riff);
        assertEquals(40, header.getInt());

        byte[] wave = new byte[4];
        header.get(wave);
        assertArrayEquals("WAVE".getBytes(StandardCharsets.US_ASCII), wave);
        header.position(22);
        assertEquals(1, header.getShort());
        assertEquals(24000, header.getInt());
        header.position(40);
        assertEquals(pcm.length, header.getInt());
        assertArrayEquals(pcm, java.util.Arrays.copyOfRange(wav, 44, wav.length));
    }
}

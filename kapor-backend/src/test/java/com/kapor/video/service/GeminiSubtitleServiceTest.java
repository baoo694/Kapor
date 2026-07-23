package com.kapor.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.video.model.Video;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiSubtitleServiceTest {

    @Test
    void usesTheGenerateContentStructuredOutputFields() {
        GeminiSubtitleService service = new GeminiSubtitleService(WebClient.builder(), new ObjectMapper());
        Video.SubtitleLine subtitle = Video.SubtitleLine.builder().text("안녕하세요").build();

        JsonNode body = ReflectionTestUtils.invokeMethod(service, "requestBody", List.of(subtitle));
        JsonNode config = body.path("generationConfig");

        assertThat(config.path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(config.path("responseJsonSchema").path("properties").path("lines").isObject()).isTrue();
        JsonNode tokenProperties = config.path("responseJsonSchema").path("properties").path("lines")
                .path("items").path("properties").path("tokens").path("items").path("properties");
        assertThat(tokenProperties.path("pronunciation").isObject()).isTrue();
        assertThat(tokenProperties.path("meaningEn").isObject()).isTrue();
        assertThat(tokenProperties.path("definitionEn").isObject()).isTrue();
        assertThat(tokenProperties.path("exampleKo").isObject()).isTrue();
        assertThat(config.has("responseFormat")).isFalse();
    }
}

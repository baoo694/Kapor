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

    @Test
    void usesATranslationOnlySchemaForSubtitleTranslations() {
        GeminiSubtitleService service = new GeminiSubtitleService(WebClient.builder(), new ObjectMapper());
        List<GeminiSubtitleService.TranslationGroup> groups = List.of(new GeminiSubtitleService.TranslationGroup(List.of(
                new GeminiSubtitleService.TranslationSourceLine(0, "안녕하세요. 저는 이 프로젝트 관리"),
                new GeminiSubtitleService.TranslationSourceLine(1, "팀의 팀장 김민수입니다.")
        )));

        JsonNode body = ReflectionTestUtils.invokeMethod(service, "groupedTranslationRequestBody", groups);
        JsonNode lineProperties = body.path("generationConfig").path("responseJsonSchema")
                .path("properties").path("lines").path("items").path("properties");

        assertThat(lineProperties.path("index").isObject()).isTrue();
        assertThat(lineProperties.path("vietnamese").isObject()).isTrue();
        assertThat(lineProperties.has("tokens")).isFalse();
        String prompt = body.path("contents").path(0).path("parts").path(0).path("text").asText();
        assertThat(prompt).contains("fragments of one spoken Korean sentence")
                .contains("Never repeat words or meaning")
                .contains("\"index\":0")
                .contains("\"index\":1");
    }

    @Test
    void usesATokenizationOnlySchemaForSubtitleTokens() {
        GeminiSubtitleService service = new GeminiSubtitleService(WebClient.builder(), new ObjectMapper());
        Video.SubtitleLine subtitle = Video.SubtitleLine.builder().text("안녕하세요").build();

        JsonNode body = ReflectionTestUtils.invokeMethod(service, "tokenizationRequestBody", List.of(subtitle));
        JsonNode lineProperties = body.path("generationConfig").path("responseJsonSchema")
                .path("properties").path("lines").path("items").path("properties");
        JsonNode tokenProperties = lineProperties.path("tokens").path("items").path("properties");

        assertThat(lineProperties.path("index").isObject()).isTrue();
        assertThat(lineProperties.has("vietnamese")).isFalse();
        assertThat(tokenProperties.path("exampleKo").isObject()).isTrue();
    }

    @Test
    void makesEveryHangulTokenClickableEvenWhenGeminiMarksItOtherwise() throws Exception {
        GeminiSubtitleService service = new GeminiSubtitleService(WebClient.builder(), new ObjectMapper());
        JsonNode tokens = new ObjectMapper().readTree("""
                [{"surface":"학교에서","clickable":false}, {"surface":"API","clickable":false}]
                """);

        @SuppressWarnings("unchecked")
        List<Video.TokenizedWord> parsed = (List<Video.TokenizedWord>) ReflectionTestUtils.invokeMethod(
                service, "parseTokens", tokens);

        assertThat(parsed).extracting(Video.TokenizedWord::isClickable).containsExactly(true, false);
    }
}

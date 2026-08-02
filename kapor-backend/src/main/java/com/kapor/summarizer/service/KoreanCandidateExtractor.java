package com.kapor.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.summarizer.SummarizerNlpException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Delegates Korean POS tagging and lemma extraction to the local NLP service. */
@Component
@RequiredArgsConstructor
public class KoreanCandidateExtractor {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${nlp.service-url:http://localhost:8001}")
    private String serviceUrl;

    public List<Candidate> extract(String text) {
        try {
            JsonNode body = objectMapper.createObjectNode().put("text", text == null ? "" : text);
            JsonNode response = webClientBuilder.build().post()
                    .uri(normalizedServiceUrl() + "/korean/candidates")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                    .retrieve().bodyToMono(JsonNode.class).block(REQUEST_TIMEOUT);
            if (response == null || !response.path("candidates").isArray()) {
                throw new SummarizerNlpException("Bộ phân tích từ vựng tiếng Hàn trả về dữ liệu không hợp lệ.");
            }
            List<Candidate> candidates = new ArrayList<>();
            for (JsonNode item : response.path("candidates")) {
                String lemma = item.path("lemma").asText("").trim();
                if (!lemma.isBlank()) candidates.add(new Candidate(lemma,
                        item.path("surface").asText(lemma).trim(), item.path("pos").asText("unknown").trim(),
                        item.path("count").asInt(1)));
            }
            return candidates;
        } catch (SummarizerNlpException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SummarizerNlpException("Không thể phân tích từ vựng tiếng Hàn. Hãy thử lại sau.", exception);
        }
    }

    private String normalizedServiceUrl() {
        return serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
    }

    public record Candidate(String lemma, String surface, String pos, int count) { }
}

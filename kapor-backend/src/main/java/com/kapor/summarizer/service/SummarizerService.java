package com.kapor.summarizer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kapor.membyte.model.MembyteDeck;
import com.kapor.membyte.model.MembyteFlashcard;
import com.kapor.membyte.repository.MembyteDeckRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.summarizer.SummarizerRateLimitException;
import com.kapor.summarizer.dto.SummarizerCardDto;
import com.kapor.summarizer.dto.SummarizerPreviewDto;
import com.kapor.summarizer.dto.SummarizerSaveDeckDto;
import com.kapor.summarizer.dto.SummarizerSaveDeckRequest;
import com.kapor.video.exception.GeminiApiException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Generates private MemByte flashcard drafts from public Korean IT material. */
@Service
@RequiredArgsConstructor
public class SummarizerService {
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration ARTICLE_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_ARTICLE_BYTES = 1_000_000;
    private static final Pattern HANGUL = Pattern.compile("[\\p{IsHangul}]");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final MembyteDeckRepository deckRepository;
    private final MembyteFlashcardRepository flashcardRepository;
    private final Map<String, Deque<Instant>> generationTimes = new ConcurrentHashMap<>();
    private final HttpClient articleClient = HttpClient.newBuilder()
            .connectTimeout(ARTICLE_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${summarizer.model-name:gemini-2.5-flash}")
    private String modelName;

    @Value("${summarizer.max-generations-per-hour:10}")
    private int maxGenerationsPerHour;

    @Value("${summarizer.max-source-characters:12000}")
    private int maxSourceCharacters;

    public SummarizerPreviewDto generate(String userId, String input, int maxCards) {
        SourceContent source = sourceFrom(input);
        if (!HANGUL.matcher(source.text()).find()) {
            throw new IllegalArgumentException("Nguồn cần có nội dung tiếng Hàn để tạo flashcard.");
        }
        enforceRateLimit(userId);
        List<SummarizerCardDto> cards = parseCards(callGemini(source, maxCards), maxCards);
        if (cards.size() < 3) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY,
                    "AI chưa tạo đủ thẻ hợp lệ. Vui lòng thử lại với bài viết chi tiết hơn.");
        }
        return SummarizerPreviewDto.builder()
                .sourceType(source.type()).sourceUrl(source.url()).title(source.title())
                .sourceExcerpt(source.text()).cards(cards).build();
    }

    public SummarizerSaveDeckDto saveDeck(String userId, SummarizerSaveDeckRequest request) {
        List<SummarizerCardDto> cards = validateCards(request.getCards());
        Instant now = Instant.now();
        String sourceId = "summarizer:" + UUID.randomUUID();
        String sourceTitle = trimTo(request.getSourceTitle(), 160);
        String title = trimTo(request.getTitle(), 120);
        if (title.isBlank()) title = "AI Summary · " + (sourceTitle.isBlank() ? "Bài viết tiếng Hàn" : sourceTitle);
        String sourceUrl = trimTo(request.getSourceUrl(), 2048);
        String excerpt = trimTo(request.getSourceExcerpt(), Math.max(1, maxSourceCharacters));
        MembyteDeck deck = deckRepository.save(MembyteDeck.builder()
                .userId(userId).lessonId(sourceId).domain("general")
                .title(title).titleVi(title).sourceType(sourceUrl.isBlank() ? "TEXT" : "URL")
                .sourceUrl(sourceUrl.isBlank() ? null : sourceUrl)
                .sourceTitle(sourceTitle.isBlank() ? null : sourceTitle)
                .sourceTextHash(excerpt.isBlank() ? null : sha256(excerpt))
                .createdAt(now).updatedAt(now).build());
        List<MembyteFlashcard> stored = cards.stream().map(card -> MembyteFlashcard.builder()
                .userId(userId).deckId(deck.getId()).lessonId(sourceId)
                .vocabularyId(normalize(card.getKorean())).korean(card.getKorean())
                .pronunciation(blankToNull(card.getPronunciation())).vietnamese(card.getVietnamese())
                .english(blankToNull(card.getEnglish())).definitionEn(blankToNull(card.getDefinitionEn()))
                .exampleKo(blankToNull(card.getExampleKo())).grammarNote(blankToNull(card.getGrammarNote()))
                .context(blankToNull(card.getContext())).isNew(true).repetitions(0).lapses(0)
                .difficulty(0).stability(0).createdAt(now).build()).toList();
        flashcardRepository.saveAll(stored);
        return SummarizerSaveDeckDto.builder().deckId(deck.getId()).savedCards(stored.size()).build();
    }

    private SourceContent sourceFrom(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.startsWith("https://") || input.startsWith("http://")) return extractPublicArticle(input);
        return new SourceContent("TEXT", null, "Bài viết đã dán", truncate(input, maxSourceCharacters));
    }

    private SourceContent extractPublicArticle(String input) {
        try {
            URI current = URI.create(input);
            for (int redirect = 0; redirect <= 3; redirect++) {
                validatePublicUri(current);
                HttpRequest request = HttpRequest.newBuilder(current).timeout(ARTICLE_TIMEOUT)
                        .header("User-Agent", "KaporSmartSummarizer/1.0")
                        .GET().build();
                HttpResponse<byte[]> response = articleClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalArgumentException("URL chuyển hướng không hợp lệ."));
                    current = current.resolve(location);
                    continue;
                }
                if (status < 200 || status >= 300) throw new IllegalArgumentException("Không thể tải bài viết từ URL này.");
                String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
                if (!contentType.contains("text/html")) throw new IllegalArgumentException("URL phải trỏ tới một trang HTML công khai.");
                if (response.body().length > MAX_ARTICLE_BYTES) throw new IllegalArgumentException("Bài viết quá lớn. Hãy dán phần nội dung cần học.");
                Document document = Jsoup.parse(new String(response.body(), StandardCharsets.UTF_8), current.toString());
                document.select("script, style, nav, footer, header, aside, form").remove();
                Element main = document.select("article, main, [role=main]").stream()
                        .max(Comparator.comparingInt(element -> element.text().length())).orElse(document.body());
                String text = truncate(main == null ? "" : main.text(), maxSourceCharacters);
                if (text.length() < 80) throw new IllegalArgumentException("Không trích xuất đủ nội dung từ URL. Hãy dán văn bản trực tiếp.");
                String title = trimTo(document.title(), 160);
                return new SourceContent("URL", current.toString(), title.isBlank() ? "Bài viết tiếng Hàn" : title, text);
            }
            throw new IllegalArgumentException("URL chuyển hướng quá nhiều lần.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Không thể tải bài viết từ URL này. Hãy dán nội dung trực tiếp.");
        }
    }

    private void validatePublicUri(URI uri) throws Exception {
        if (uri.getUserInfo() != null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("URL không hợp lệ.");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("URL nội bộ không được hỗ trợ.");
            }
        }
    }

    private JsonNode callGemini(SourceContent source, int maxCards) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "SmartSummarizer chưa được cấu hình trên máy chủ.");
        }
        try {
            ObjectNode request = objectMapper.createObjectNode();
            ArrayNode parts = request.putArray("contents").addObject().putArray("parts");
            parts.addObject().put("text", prompt(source, maxCards));
            ObjectNode config = request.putObject("generationConfig");
            config.put("temperature", 0.2);
            config.put("responseMimeType", "application/json");
            config.set("responseJsonSchema", cardSchema());
            return webClientBuilder.build().post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", modelName)
                    .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                    .retrieve().onStatus(HttpStatusCode::isError, response -> response.bodyToMono(JsonNode.class)
                            .defaultIfEmpty(objectMapper.createObjectNode()).map(body -> geminiError(response.statusCode(), body)))
                    .bodyToMono(JsonNode.class).block(AI_TIMEOUT);
        } catch (GeminiApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "Không thể kết nối SmartSummarizer. Vui lòng thử lại.", exception);
        }
    }

    private String prompt(SourceContent source, int maxCards) {
        return """
                You are a Korean IT vocabulary educator. Create %d useful, distinct flashcards from the source below.
                Use only facts and vocabulary that appear in the source. Prefer Korean IT terms, compounds, verbs, and useful grammar; do not create generic filler cards.
                Every korean field must contain Hangul and be a concise learner-facing headword. Provide natural Vietnamese and English meanings, Revised Romanization, a short English definition, a natural Korean example, and a source-grounded context. grammarNote may be empty when irrelevant.
                Do not use Markdown and do not add text outside the JSON schema.
                Source title: %s
                Source:
                %s
                """.formatted(maxCards, source.title(), source.text());
    }

    private ObjectNode cardSchema() {
        try {
            return (ObjectNode) objectMapper.readTree("""
                    {"type":"object","properties":{"cards":{"type":"array","items":{"type":"object","properties":{
                    "korean":{"type":"string"},"pronunciation":{"type":"string"},"vietnamese":{"type":"string"},"english":{"type":"string"},
                    "definitionEn":{"type":"string"},"exampleKo":{"type":"string"},"grammarNote":{"type":["string","null"]},"context":{"type":"string"}
                    },"required":["korean","pronunciation","vietnamese","english","definitionEn","exampleKo","grammarNote","context"]}}},"required":["cards"]}
                    """);
        } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot build SmartSummarizer schema", exception); }
    }

    private List<SummarizerCardDto> parseCards(JsonNode response, int maxCards) {
        JsonNode text = response == null ? null : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || !text.isTextual()) throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "AI không trả về flashcard hợp lệ.");
        try {
            JsonNode cards = objectMapper.readTree(text.asText()).path("cards");
            if (!cards.isArray()) throw new IllegalArgumentException("AI returned invalid cards");
            List<SummarizerCardDto> parsed = new ArrayList<>();
            for (JsonNode card : cards) parsed.add(SummarizerCardDto.builder()
                    .korean(trimTo(card.path("korean").asText(), 120)).pronunciation(trimTo(card.path("pronunciation").asText(), 160))
                    .vietnamese(trimTo(card.path("vietnamese").asText(), 400)).english(trimTo(card.path("english").asText(), 400))
                    .definitionEn(trimTo(card.path("definitionEn").asText(), 800)).exampleKo(trimTo(card.path("exampleKo").asText(), 800))
                    .grammarNote(card.path("grammarNote").isNull() ? null : trimTo(card.path("grammarNote").asText(), 800))
                    .context(trimTo(card.path("context").asText(), 1200)).build());
            return validateCards(parsed).stream().limit(maxCards).toList();
        } catch (RuntimeException | JsonProcessingException exception) {
            throw new GeminiApiException(HttpStatus.BAD_GATEWAY, "AI trả về flashcard không hợp lệ. Vui lòng thử lại.", exception);
        }
    }

    private List<SummarizerCardDto> validateCards(List<SummarizerCardDto> cards) {
        if (cards == null) throw new IllegalArgumentException("Hãy chọn ít nhất một thẻ để lưu.");
        Set<String> words = new HashSet<>();
        List<SummarizerCardDto> valid = new ArrayList<>();
        for (SummarizerCardDto card : cards) {
            if (card == null) continue;
            card.setKorean(trimTo(card.getKorean(), 120)); card.setVietnamese(trimTo(card.getVietnamese(), 400));
            if (card.getKorean().isBlank() || card.getVietnamese().isBlank() || !HANGUL.matcher(card.getKorean()).find()) continue;
            if (words.add(normalize(card.getKorean()))) valid.add(card);
        }
        if (valid.isEmpty()) throw new IllegalArgumentException("Không có flashcard tiếng Hàn hợp lệ để lưu.");
        return valid;
    }

    private void enforceRateLimit(String userId) {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        Deque<Instant> attempts = generationTimes.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) attempts.removeFirst();
            if (attempts.size() >= Math.max(1, maxGenerationsPerHour)) throw new SummarizerRateLimitException("Bạn đã dùng hết lượt tạo flashcard trong giờ này. Vui lòng thử lại sau.");
            attempts.addLast(Instant.now());
        }
    }

    private GeminiApiException geminiError(HttpStatusCode status, JsonNode body) {
        if (status.value() == 429) return new GeminiApiException(HttpStatus.TOO_MANY_REQUESTS, "SmartSummarizer đang bận. Vui lòng thử lại sau.");
        if (status.value() == 401 || status.value() == 403) return new GeminiApiException(HttpStatus.BAD_GATEWAY, "SmartSummarizer không được cấu hình đúng API key.");
        String message = body.path("error").path("message").asText("").replaceAll("[\\r\\n]+", " ").trim();
        return new GeminiApiException(HttpStatus.BAD_GATEWAY, message.isBlank() ? "AI không thể tạo flashcard." : "AI không thể tạo flashcard: " + message);
    }

    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("Cannot hash SmartSummarizer source", exception); }
    }
    private static String normalize(String value) { return value == null ? "" : value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static String truncate(String value, int limit) { String safe = value == null ? "" : value.trim(); return safe.length() <= limit ? safe : safe.substring(0, limit); }
    private static String trimTo(String value, int limit) { return truncate(value, limit); }

    private record SourceContent(String type, String url, String title, String text) { }
}

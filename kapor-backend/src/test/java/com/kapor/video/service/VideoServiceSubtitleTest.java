package com.kapor.video.service;

import com.kapor.video.dto.SubtitleUpdateRequest;
import com.kapor.video.dto.SubtitleTokenizeRequest;
import com.kapor.video.dto.SubtitleAiAnalyzeRequest;
import com.kapor.video.dto.VideoDto;
import com.kapor.video.model.Video;
import com.kapor.video.repository.VideoRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoServiceSubtitleTest {

    @Test
    void savesValidatedPairedSubtitles() {
        AtomicReference<Video> saved = new AtomicReference<>();
        Video video = Video.builder().id("video-1").build();
        VideoService service = serviceFor(video, saved);

        VideoDto result = service.updateSubtitles("video-1", request(
                line(0, 2, "안녕하세요"), line(2, 4, "반갑습니다"),
                line(0, 2, "Xin chào"), line(2, 4, "Rất vui được gặp bạn")
        ));

        assertThat(result.getKoreanSubtitles()).hasSize(2);
        assertThat(saved.get().getVietnameseSubtitles().get(1).getText()).isEqualTo("Rất vui được gặp bạn");
    }

    @Test
    void rejectsOverlappingSubtitleLines() {
        VideoService service = serviceFor(Video.builder().id("video-1").build(), new AtomicReference<>());

        assertThatThrownBy(() -> service.updateSubtitles("video-1", request(
                line(0, 3, "첫 번째"), line(2, 4, "두 번째"),
                line(0, 3, "Dòng một"), line(2, 4, "Dòng hai")
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void tokenizesKoreanAndKeepsVietnameseTokensEmpty() {
        AtomicReference<Video> saved = new AtomicReference<>();
        VideoService service = serviceFor(Video.builder().id("video-1").build(), saved);

        VideoDto result = service.tokenizeSubtitles("video-1", request(
                line(0, 2, "안녕하세요, 개발자!"),
                line(0, 2, "Xin chào, lập trình viên!")
        ));

        assertThat(result.getKoreanSubtitles().get(0).getTokens())
                .extracting(Video.TokenizedWord::getSurface)
                .containsExactly("안녕하세요", "개발자");
        assertThat(result.getKoreanSubtitles().get(0).getTokens().get(0).isClickable()).isTrue();
        assertThat(saved.get().getVietnameseSubtitles().get(0).getTokens()).isEmpty();
    }

    @Test
    void tokenizesKoreanDraftBeforeVietnameseTranslationExists() {
        AtomicReference<Video> saved = new AtomicReference<>();
        VideoService service = serviceFor(Video.builder().id("video-1").build(), saved);
        SubtitleTokenizeRequest request = new SubtitleTokenizeRequest();
        request.setKoreanSubtitles(List.of(line(0, 2, "안녕하세요 개발자")));

        VideoDto result = service.tokenizeKoreanSubtitles("video-1", request);

        assertThat(result.getKoreanSubtitles().get(0).getTokens()).hasSize(2);
        assertThat(saved.get().getVietnameseSubtitles()).isEmpty();
    }

    @Test
    void aiAnalysisCreatesVietnamesePairsAndMeaningfulTokens() {
        AtomicReference<Video> saved = new AtomicReference<>();
        GeminiSubtitleService ai = new GeminiSubtitleService(null, null) {
            @Override
            public List<AnalysisLine> analyze(List<Video.SubtitleLine> subtitles) {
                return List.of(new AnalysisLine(0, "Xin chào mọi người", List.of(
                        Video.TokenizedWord.builder().surface("안녕하세요").stem("안녕하세요").pos("INTJ")
                                .meaningVi("xin chào").clickable(true).build())));
            }
        };
        VideoService service = serviceFor(Video.builder().id("video-1").build(), saved, ai);
        SubtitleAiAnalyzeRequest request = new SubtitleAiAnalyzeRequest();
        request.setKoreanSubtitles(List.of(line(0, 2, "안녕하세요 여러분")));

        VideoDto result = service.analyzeSubtitlesWithAi("video-1", request);

        assertThat(result.getVietnameseSubtitles().get(0).getText()).isEqualTo("Xin chào mọi người");
        assertThat(result.getKoreanSubtitles().get(0).getTokens().get(0).getMeaningVi()).isEqualTo("xin chào");
    }

    private VideoService serviceFor(Video video, AtomicReference<Video> saved) {
        return serviceFor(video, saved, null);
    }

    private VideoService serviceFor(Video video, AtomicReference<Video> saved, GeminiSubtitleService geminiSubtitleService) {
        VideoRepository repository = (VideoRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{VideoRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(video);
                    case "save" -> { saved.set((Video) args[0]); yield args[0]; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new VideoService(repository, new SubtitleTokenizer(), geminiSubtitleService);
    }

    private SubtitleUpdateRequest request(Video.SubtitleLine... lines) {
        int half = lines.length / 2;
        SubtitleUpdateRequest request = new SubtitleUpdateRequest();
        request.setKoreanSubtitles(List.of(lines).subList(0, half));
        request.setVietnameseSubtitles(List.of(lines).subList(half, lines.length));
        return request;
    }

    private Video.SubtitleLine line(double start, double end, String text) {
        return Video.SubtitleLine.builder().start(start).end(end).text(text).build();
    }
}

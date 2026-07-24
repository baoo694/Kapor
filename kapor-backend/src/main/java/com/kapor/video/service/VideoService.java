package com.kapor.video.service;

import com.kapor.video.dto.VideoDto;
import com.kapor.video.dto.SubtitleUpdateRequest;
import com.kapor.video.dto.SubtitleTokenizeRequest;
import com.kapor.video.dto.SubtitleAiAnalyzeRequest;
import com.kapor.video.dto.SubtitleTranslateRequest;
import com.kapor.video.model.Video;
import com.kapor.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final SubtitleTokenizer subtitleTokenizer;
    private final GeminiSubtitleService geminiSubtitleService;

    @Value("${gemini.subtitle-batch-size:12}")
    private int geminiSubtitleBatchSize;

    public List<VideoDto> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(VideoDto::fromEntity)
                .collect(Collectors.toList());
    }

    public VideoDto createVideo(VideoDto dto) {
        Video video = Video.builder()
                .title(dto.getTitle())
                .titleVi(dto.getTitleVi())
                .youtubeUrl(dto.getYoutubeUrl())
                .youtubeVideoId(nonBlank(dto.getYoutubeVideoId(), extractYoutubeId(dto.getYoutubeUrl())))
                .thumbnailUrl(dto.getThumbnailUrl())
                .domain(dto.getDomain())
                .difficulty(dto.getDifficulty())
                .durationSeconds(dto.getDurationSeconds())
                .koreanSubtitles(dto.getKoreanSubtitles())
                .vietnameseSubtitles(dto.getVietnameseSubtitles())
                .quizMarkers(dto.getQuizMarkers())
                .build();
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public VideoDto updateVideo(String id, VideoDto dto) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setTitle(dto.getTitle());
        video.setTitleVi(dto.getTitleVi());
        video.setYoutubeUrl(dto.getYoutubeUrl());
        video.setYoutubeVideoId(nonBlank(dto.getYoutubeVideoId(), extractYoutubeId(dto.getYoutubeUrl())));
        video.setThumbnailUrl(dto.getThumbnailUrl());
        video.setDomain(dto.getDomain());
        video.setDifficulty(dto.getDifficulty());
        video.setDurationSeconds(dto.getDurationSeconds());
        video.setKoreanSubtitles(dto.getKoreanSubtitles());
        video.setVietnameseSubtitles(dto.getVietnameseSubtitles());
        video.setQuizMarkers(dto.getQuizMarkers());
        video = videoRepository.save(video);
        return VideoDto.fromEntity(video);
    }

    public VideoDto updateSubtitles(String id, SubtitleUpdateRequest request) {
        Video video = findVideo(id);
        validateSubtitles(request.getKoreanSubtitles(), request.getVietnameseSubtitles());
        video.setKoreanSubtitles(request.getKoreanSubtitles());
        video.setVietnameseSubtitles(request.getVietnameseSubtitles());
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public VideoDto tokenizeSubtitles(String id, SubtitleUpdateRequest request) {
        Video video = findVideo(id);
        validateSubtitles(request.getKoreanSubtitles(), request.getVietnameseSubtitles());
        request.getKoreanSubtitles().forEach(line -> line.setTokens(subtitleTokenizer.tokenize(line.getText())));
        request.getVietnameseSubtitles().forEach(line -> line.setTokens(List.of()));
        video.setKoreanSubtitles(request.getKoreanSubtitles());
        video.setVietnameseSubtitles(request.getVietnameseSubtitles());
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public VideoDto tokenizeKoreanSubtitles(String id, SubtitleTokenizeRequest request) {
        Video video = findVideo(id);
        validateKoreanSubtitles(request.getKoreanSubtitles());
        request.getKoreanSubtitles().forEach(line -> line.setTokens(subtitleTokenizer.tokenize(line.getText())));
        video.setKoreanSubtitles(request.getKoreanSubtitles());
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public VideoDto tokenizeKoreanSubtitlesWithAi(String id, SubtitleTokenizeRequest request) {
        Video video = findVideo(id);
        List<Video.SubtitleLine> korean = request.getKoreanSubtitles();
        validateKoreanSubtitles(korean);
        int batchSize = Math.max(1, geminiSubtitleBatchSize);

        for (int start = 0; start < korean.size(); start += batchSize) {
            int end = Math.min(start + batchSize, korean.size());
            List<Video.SubtitleLine> batch = korean.subList(start, end);
            List<GeminiSubtitleService.TokenizationLine> tokenization = geminiSubtitleService.tokenize(batch);
            for (GeminiSubtitleService.TokenizationLine result : tokenization) {
                Video.SubtitleLine koreanLine = batch.get(result.index());
                koreanLine.setTokens(result.tokens().stream()
                        .filter(token -> koreanLine.getText().contains(token.getSurface()))
                        .collect(Collectors.toList()));
            }
        }

        video.setKoreanSubtitles(korean);
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public VideoDto translateSubtitlesWithAi(String id, SubtitleTranslateRequest request) {
        Video video = findVideo(id);
        List<Video.SubtitleLine> korean = request.getKoreanSubtitles();
        validateKoreanSubtitles(korean);
        List<Video.SubtitleLine> vietnamese = new java.util.ArrayList<>();
        int batchSize = Math.max(1, geminiSubtitleBatchSize);

        for (int start = 0; start < korean.size(); start += batchSize) {
            int end = Math.min(start + batchSize, korean.size());
            List<Video.SubtitleLine> batch = korean.subList(start, end);
            List<GeminiSubtitleService.TranslationLine> translations = geminiSubtitleService.translate(batch);
            for (GeminiSubtitleService.TranslationLine result : translations) {
                Video.SubtitleLine koreanLine = batch.get(result.index());
                vietnamese.add(Video.SubtitleLine.builder()
                        .start(koreanLine.getStart())
                        .end(koreanLine.getEnd())
                        .text(result.vietnamese())
                        .tokens(List.of())
                        .build());
            }
        }

        if (vietnamese.size() != korean.size()) {
            throw new IllegalStateException("Gemini did not return every subtitle translation");
        }
        video.setKoreanSubtitles(korean);
        video.setVietnameseSubtitles(vietnamese);
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public VideoDto analyzeSubtitlesWithAi(String id, SubtitleAiAnalyzeRequest request) {
        Video video = findVideo(id);
        List<Video.SubtitleLine> korean = request.getKoreanSubtitles();
        validateKoreanSubtitles(korean);
        List<Video.SubtitleLine> vietnamese = new java.util.ArrayList<>();
        int batchSize = Math.max(1, geminiSubtitleBatchSize);

        for (int start = 0; start < korean.size(); start += batchSize) {
            int end = Math.min(start + batchSize, korean.size());
            List<Video.SubtitleLine> batch = korean.subList(start, end);
            List<GeminiSubtitleService.AnalysisLine> analysis = geminiSubtitleService.analyze(batch);
            for (GeminiSubtitleService.AnalysisLine result : analysis) {
                Video.SubtitleLine koreanLine = batch.get(result.index());
                List<Video.TokenizedWord> verifiedTokens = result.tokens().stream()
                        .filter(token -> koreanLine.getText().contains(token.getSurface()))
                        .collect(Collectors.toList());
                koreanLine.setTokens(verifiedTokens);
                vietnamese.add(Video.SubtitleLine.builder()
                        .start(koreanLine.getStart())
                        .end(koreanLine.getEnd())
                        .text(result.vietnamese())
                        .tokens(List.of())
                        .build());
            }
        }

        if (vietnamese.size() != korean.size()) {
            throw new IllegalStateException("Gemini did not return every subtitle line");
        }
        video.setKoreanSubtitles(korean);
        video.setVietnameseSubtitles(vietnamese);
        return VideoDto.fromEntity(videoRepository.save(video));
    }

    public void deleteVideo(String id) {
        videoRepository.deleteById(id);
    }

    public VideoDto getVideo(String id) {
        return VideoDto.fromEntity(findVideo(id));
    }

    public List<VideoDto> getVideos(String domain) {
        return videoRepository.findAll().stream()
                .filter(video -> domain == null || domain.isBlank() || domain.equalsIgnoreCase(video.getDomain()))
                .map(VideoDto::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean answerQuiz(String videoId, String quizId, int answer) {
        Video video = findVideo(videoId);
        Video.QuizMarker marker = video.getQuizMarkers().stream()
                .filter(item -> quizId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new com.kapor.common.exception.ResourceNotFoundException("Quiz", "id", quizId));
        return marker.getCorrectAnswer() != null && marker.getCorrectAnswer() == answer;
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) return null;
        String value = url.trim();
        if (!value.contains("/")) return value;
        int query = value.indexOf("v=");
        if (query >= 0) {
            String id = value.substring(query + 2);
            return id.split("[&#?]")[0];
        }
        String[] parts = value.split("/");
        return parts.length == 0 ? null : parts[parts.length - 1].split("[?#]")[0];
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Video findVideo(String id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new com.kapor.common.exception.ResourceNotFoundException("Video", "id", id));
    }

    private void validateSubtitles(List<Video.SubtitleLine> korean, List<Video.SubtitleLine> vietnamese) {
        if (korean == null || vietnamese == null) {
            throw new IllegalArgumentException("Both Korean and Vietnamese subtitles are required");
        }
        if (korean.size() != vietnamese.size()) {
            throw new IllegalArgumentException("Each Korean subtitle must have one Vietnamese subtitle");
        }
        double previousEnd = -1;
        for (int index = 0; index < korean.size(); index++) {
            Video.SubtitleLine ko = korean.get(index);
            Video.SubtitleLine vi = vietnamese.get(index);
            if (ko == null || vi == null || ko.getText() == null || ko.getText().isBlank()
                    || vi.getText() == null || vi.getText().isBlank()) {
                throw new IllegalArgumentException("Subtitle line " + (index + 1) + " must contain Korean and Vietnamese text");
            }
            if (!Double.isFinite(ko.getStart()) || !Double.isFinite(ko.getEnd())
                    || !Double.isFinite(vi.getStart()) || !Double.isFinite(vi.getEnd())
                    || ko.getStart() < 0 || ko.getEnd() <= ko.getStart()) {
                throw new IllegalArgumentException("Subtitle line " + (index + 1) + " has an invalid timestamp");
            }
            if (Math.abs(ko.getStart() - vi.getStart()) > 0.001 || Math.abs(ko.getEnd() - vi.getEnd()) > 0.001) {
                throw new IllegalArgumentException("Korean and Vietnamese timestamps must match on line " + (index + 1));
            }
            if (ko.getStart() < previousEnd) {
                throw new IllegalArgumentException("Subtitle line " + (index + 1) + " overlaps the previous line");
            }
            previousEnd = ko.getEnd();
        }
    }

    private void validateKoreanSubtitles(List<Video.SubtitleLine> korean) {
        if (korean == null || korean.isEmpty()) {
            throw new IllegalArgumentException("At least one Korean subtitle is required for tokenization");
        }
        double previousEnd = -1;
        for (int index = 0; index < korean.size(); index++) {
            Video.SubtitleLine line = korean.get(index);
            if (line == null || line.getText() == null || line.getText().isBlank()) {
                throw new IllegalArgumentException("Korean subtitle line " + (index + 1) + " must contain text");
            }
            if (!Double.isFinite(line.getStart()) || !Double.isFinite(line.getEnd())
                    || line.getStart() < 0 || line.getEnd() <= line.getStart()) {
                throw new IllegalArgumentException("Korean subtitle line " + (index + 1) + " has an invalid timestamp");
            }
            if (line.getStart() < previousEnd) {
                throw new IllegalArgumentException("Korean subtitle line " + (index + 1) + " overlaps the previous line");
            }
            previousEnd = line.getEnd();
        }
    }
}

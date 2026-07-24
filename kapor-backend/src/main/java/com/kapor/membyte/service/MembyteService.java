package com.kapor.membyte.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.devvocab.model.Lesson;
import com.kapor.devvocab.model.Topic;
import com.kapor.devvocab.repository.LessonRepository;
import com.kapor.devvocab.repository.TopicRepository;
import com.kapor.membyte.dto.MembyteDeckSummaryDto;
import com.kapor.membyte.dto.MembyteReviewCardDto;
import com.kapor.membyte.dto.MembyteReviewRatingRequest;
import com.kapor.membyte.dto.MembyteReviewResultDto;
import com.kapor.membyte.dto.MembyteReviewSummaryDto;
import com.kapor.membyte.dto.MembyteSaveCardDto;
import com.kapor.membyte.dto.MembyteSavedCardsDto;
import com.kapor.membyte.model.MembyteDeck;
import com.kapor.membyte.model.MembyteFlashcard;
import com.kapor.membyte.repository.MembyteDeckRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.video.model.Video;
import com.kapor.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembyteService {

    private final MembyteDeckRepository deckRepository;
    private final MembyteFlashcardRepository flashcardRepository;
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final VideoRepository videoRepository;
    private final FsrsService fsrsService;

    public MembyteSaveCardDto saveVocabulary(String userId, String lessonId, String vocabularyId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        Lesson.VocabularyItem vocabulary = lesson.getVocabulary() == null ? null : lesson.getVocabulary().stream()
                .filter(item -> vocabularyId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (vocabulary == null) {
            throw new ResourceNotFoundException("Vocabulary", "id", vocabularyId);
        }

        MembyteFlashcard existing = flashcardRepository
                .findByUserIdAndLessonIdAndVocabularyId(userId, lessonId, vocabularyId)
                .orElse(null);
        if (existing != null) {
            return MembyteSaveCardDto.builder()
                    .deckId(existing.getDeckId())
                    .cardId(existing.getId())
                    .vocabularyId(vocabularyId)
                    .alreadySaved(true)
                    .build();
        }

        Topic topic = topicRepository.findById(lesson.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", lesson.getTopicId()));
        Instant now = Instant.now();
        MembyteDeck deck = deckRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> deckRepository.save(MembyteDeck.builder()
                        .userId(userId)
                        .topicId(topic.getId())
                        .lessonId(lesson.getId())
                        .domain(topic.getDomain())
                        .title(joinLabels(topic.getTitle(), lesson.getTitle()))
                        .titleVi(joinLabels(topic.getTitleVi(), lesson.getTitleVi()))
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));

        MembyteFlashcard saved = flashcardRepository.save(MembyteFlashcard.builder()
                .userId(userId)
                .deckId(deck.getId())
                .lessonId(lesson.getId())
                .vocabularyId(vocabulary.getId())
                .korean(vocabulary.getKorean())
                .pronunciation(vocabulary.getPronunciation())
                .vietnamese(vocabulary.getVietnamese())
                .english(vocabulary.getEnglish())
                .context(vocabulary.getContext())
                .codeSnippet(vocabulary.getCodeSnippet())
                .audioUrl(vocabulary.getAudioUrl())
                .isNew(true)
                .repetitions(0)
                .lapses(0)
                .difficulty(0)
                .stability(0)
                .createdAt(now)
                .build());
        deck.setUpdatedAt(now);
        deckRepository.save(deck);

        return MembyteSaveCardDto.builder()
                .deckId(deck.getId())
                .cardId(saved.getId())
                .vocabularyId(vocabularyId)
                .alreadySaved(false)
                .build();
    }

    public MembyteSaveCardDto saveVideoToken(String userId, String videoId, String surface) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));
        VideoTokenMatch tokenMatch = findVideoToken(video, surface);
        if (tokenMatch == null) {
            throw new ResourceNotFoundException("Video token", "surface", surface);
        }

        // Reuse the existing lesson-based indexes with a namespaced source id.
        // This keeps one personal deck per video without a data migration.
        String videoSourceId = "video:" + videoId;
        Instant now = Instant.now();
        MembyteDeck deck = deckRepository.findByUserIdAndLessonId(userId, videoSourceId)
                .orElseGet(() -> deckRepository.save(MembyteDeck.builder()
                        .userId(userId)
                        .lessonId(videoSourceId)
                        .domain(video.getDomain())
                        .title(video.getTitle())
                        .titleVi(video.getTitleVi())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));

        Video.TokenizedWord token = tokenMatch.token();
        String savedWord = token.getStem() == null || token.getStem().isBlank()
                ? token.getSurface()
                : token.getStem();
        MembyteFlashcard existing = flashcardRepository.findByUserIdAndLessonId(userId, videoSourceId).stream()
                .filter(card -> savedWord.equals(card.getVocabularyId())
                        || token.getSurface().equals(card.getVocabularyId())
                        || savedWord.equals(card.getKorean()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            applyVideoTokenDetails(existing, token, tokenMatch.subtitleText(), savedWord);
            flashcardRepository.save(existing);
            return MembyteSaveCardDto.builder()
                    .deckId(existing.getDeckId())
                    .cardId(existing.getId())
                    .vocabularyId(savedWord)
                    .alreadySaved(true)
                    .build();
        }
        MembyteFlashcard saved = flashcardRepository.save(MembyteFlashcard.builder()
                .userId(userId)
                .deckId(deck.getId())
                .lessonId(videoSourceId)
                .vocabularyId(savedWord)
                .korean(savedWord)
                .pronunciation(token.getPronunciation())
                .vietnamese(token.getMeaningVi())
                .english(token.getMeaningEn())
                .definitionEn(token.getDefinitionEn())
                .exampleKo(token.getExampleKo())
                .grammarNote(token.getGrammarNote())
                .context(tokenMatch.subtitleText())
                .isNew(true)
                .repetitions(0)
                .lapses(0)
                .difficulty(0)
                .stability(0)
                .createdAt(now)
                .build());
        deck.setUpdatedAt(now);
        deckRepository.save(deck);

        return MembyteSaveCardDto.builder()
                .deckId(deck.getId())
                .cardId(saved.getId())
                .vocabularyId(savedWord)
                .alreadySaved(false)
                .build();
    }

    public MembyteSavedCardsDto getSavedVocabularyIds(String userId, String lessonId) {
        Set<String> vocabularyIds = flashcardRepository.findByUserIdAndLessonId(userId, lessonId).stream()
                .map(MembyteFlashcard::getVocabularyId)
                .collect(Collectors.toSet());
        return MembyteSavedCardsDto.builder().vocabularyIds(vocabularyIds).build();
    }

    public List<MembyteDeckSummaryDto> getDecks(String userId) {
        Instant now = Instant.now();
        return deckRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(deck -> toDeckSummary(deck, flashcardRepository.findByUserIdAndDeckIdOrderByCreatedAtAsc(userId, deck.getId()), now))
                .toList();
    }

    public MembyteReviewSummaryDto getReviewSummary(String userId) {
        Instant now = Instant.now();
        Map<String, List<MembyteFlashcard>> cardsByDeck = flashcardRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .collect(Collectors.groupingBy(MembyteFlashcard::getDeckId));
        int totalDue = 0;
        int deckCount = 0;
        for (List<MembyteFlashcard> cards : cardsByDeck.values()) {
            int dueInDeck = (int) cards.stream().filter(card -> isDue(card, now)).count();
            totalDue += dueInDeck;
            if (dueInDeck > 0) deckCount++;
        }
        return MembyteReviewSummaryDto.builder()
                .totalDueCards(totalDue)
                .decksWithDueCards(deckCount)
                .build();
    }

    public List<MembyteReviewCardDto> getReviewCards(String userId, String deckId, boolean includeNew, int limit) {
        Instant now = Instant.now();
        List<MembyteFlashcard> cards;
        if (deckId == null || deckId.isBlank()) {
            cards = flashcardRepository.findByUserIdOrderByCreatedAtAsc(userId);
        } else {
            deckRepository.findById(deckId)
                    .filter(deck -> userId.equals(deck.getUserId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
            cards = flashcardRepository.findByUserIdAndDeckIdOrderByCreatedAtAsc(userId, deckId);
        }

        enrichVideoFlashcards(cards);

        return cards.stream()
                .filter(card -> isDue(card, now) || (includeNew && card.isNew()))
                .sorted(Comparator.comparing(MembyteFlashcard::isNew).thenComparing(MembyteFlashcard::getDueAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(card -> toReviewCard(card, now))
                .toList();
    }

    public MembyteReviewResultDto rateCard(String userId, MembyteReviewRatingRequest request) {
        MembyteFlashcard card = flashcardRepository.findById(request.getCardId())
                .filter(savedCard -> userId.equals(savedCard.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard", "id", request.getCardId()));
        Instant now = Instant.now();
        FsrsService.Schedule schedule = fsrsService.rate(card, request.getRating(), now);
        flashcardRepository.save(card);
        deckRepository.findById(card.getDeckId()).ifPresent(deck -> {
            deck.setUpdatedAt(now);
            deckRepository.save(deck);
        });
        return MembyteReviewResultDto.builder()
                .nextReview(schedule.getDueAt())
                .nextReviewLabel(fsrsService.formatInterval(schedule.getDueAt(), now))
                .difficulty(schedule.getDifficulty())
                .stability(schedule.getStability())
                .build();
    }

    private MembyteDeckSummaryDto toDeckSummary(MembyteDeck deck, List<MembyteFlashcard> cards, Instant now) {
        return MembyteDeckSummaryDto.builder()
                .id(deck.getId())
                .title(deck.getTitle())
                .titleVi(deck.getTitleVi())
                .domain(deck.getDomain())
                .totalCards(cards.size())
                .dueCards((int) cards.stream().filter(card -> isDue(card, now)).count())
                .newCards((int) cards.stream().filter(MembyteFlashcard::isNew).count())
                .build();
    }

    private MembyteReviewCardDto toReviewCard(MembyteFlashcard card, Instant now) {
        Map<String, String> previews = new LinkedHashMap<>();
        for (MembyteReviewRatingRequest.Rating rating : MembyteReviewRatingRequest.Rating.values()) {
            FsrsService.Schedule schedule = fsrsService.preview(card, rating, now);
            previews.put(rating.name(), fsrsService.formatInterval(schedule.getDueAt(), now));
        }
        return MembyteReviewCardDto.builder()
                .id(card.getId())
                .deckId(card.getDeckId())
                .korean(card.getKorean())
                .pronunciation(card.getPronunciation())
                .vietnamese(card.getVietnamese())
                .english(card.getEnglish())
                .definitionEn(card.getDefinitionEn())
                .exampleKo(card.getExampleKo())
                .grammarNote(card.getGrammarNote())
                .context(card.getContext())
                .codeSnippet(card.getCodeSnippet())
                .audioUrl(card.getAudioUrl())
                .nextReviewTimes(previews)
                .build();
    }

    private boolean isDue(MembyteFlashcard card, Instant now) {
        return !card.isNew() && card.getDueAt() != null && !card.getDueAt().isAfter(now);
    }

    private VideoTokenMatch findVideoToken(Video video, String surface) {
        if (video.getKoreanSubtitles() == null) return null;
        for (Video.SubtitleLine subtitle : video.getKoreanSubtitles()) {
            if (subtitle.getTokens() == null) continue;
            for (Video.TokenizedWord token : subtitle.getTokens()) {
                if (surface.equals(token.getSurface())) {
                    return new VideoTokenMatch(token, subtitle.getText());
                }
            }
        }
        return null;
    }

    private void applyVideoTokenDetails(
            MembyteFlashcard card,
            Video.TokenizedWord token,
            String subtitleText,
            String savedWord) {
        card.setVocabularyId(savedWord);
        card.setKorean(savedWord);
        card.setPronunciation(token.getPronunciation());
        card.setVietnamese(token.getMeaningVi());
        card.setEnglish(token.getMeaningEn());
        card.setDefinitionEn(token.getDefinitionEn());
        card.setExampleKo(token.getExampleKo());
        card.setGrammarNote(token.getGrammarNote());
        card.setContext(subtitleText);
    }

    private void enrichVideoFlashcards(List<MembyteFlashcard> cards) {
        for (MembyteFlashcard card : cards) {
            if (card.getLessonId() == null || !card.getLessonId().startsWith("video:")) continue;
            String videoId = card.getLessonId().substring("video:".length());
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || video.getKoreanSubtitles() == null) continue;
            VideoTokenMatch match = findVideoTokenForCard(video, card);
            if (match == null) continue;
            Video.TokenizedWord token = match.token();
            String savedWord = token.getStem() == null || token.getStem().isBlank()
                    ? token.getSurface()
                    : token.getStem();
            card.setKorean(savedWord);
            card.setPronunciation(token.getPronunciation());
            card.setVietnamese(token.getMeaningVi());
            card.setEnglish(token.getMeaningEn());
            card.setDefinitionEn(token.getDefinitionEn());
            card.setExampleKo(token.getExampleKo());
            card.setGrammarNote(token.getGrammarNote());
            card.setContext(match.subtitleText());
            flashcardRepository.save(card);
        }
    }

    private VideoTokenMatch findVideoTokenForCard(Video video, MembyteFlashcard card) {
        for (Video.SubtitleLine subtitle : video.getKoreanSubtitles()) {
            if (subtitle.getTokens() == null) continue;
            for (Video.TokenizedWord token : subtitle.getTokens()) {
                if (card.getVocabularyId().equals(token.getSurface())
                        || card.getVocabularyId().equals(token.getStem())
                        || card.getKorean().equals(token.getSurface())
                        || card.getKorean().equals(token.getStem())) {
                    return new VideoTokenMatch(token, subtitle.getText());
                }
            }
        }
        return null;
    }

    private record VideoTokenMatch(Video.TokenizedWord token, String subtitleText) {
    }

    private String joinLabels(String topicLabel, String lessonLabel) {
        List<String> labels = new ArrayList<>();
        if (topicLabel != null && !topicLabel.isBlank()) labels.add(topicLabel);
        if (lessonLabel != null && !lessonLabel.isBlank()) labels.add(lessonLabel);
        return String.join(" · ", labels);
    }
}

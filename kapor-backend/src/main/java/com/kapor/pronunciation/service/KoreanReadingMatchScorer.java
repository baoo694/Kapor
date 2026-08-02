package com.kapor.pronunciation.service;

import com.kapor.pronunciation.model.PronunciationAttempt;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scores how closely a learner's Whisper transcript matches a known Korean
 * reading prompt. This is deliberately a reading-match score, not a phoneme
 * pronunciation assessment.
 */
@Component
final class KoreanReadingMatchScorer {
    private static final double INSERTION_COST = .75d;
    private static final int CLEAR_MISMATCH_SIMILARITY = 35;
    private static final int PROBABLE_MISMATCH_SIMILARITY = 55;
    private static final int LOW_AZURE_COMPLETENESS = 50;
    /** A close ASR spelling such as 비동기 -> 피동기 still means the word was read. */
    private static final int COVERED_WORD_SIMILARITY = 55;

    PronunciationAssessmentProvider.Assessment score(String expectedText, String transcription, double durationSeconds) {
        List<String> expectedWords = words(expectedText);
        List<String> actualWords = words(transcription);
        List<WordMatch> alignment = align(expectedWords, actualWords);
        List<PronunciationAttempt.WordFeedback> feedback = alignment.stream().map(this::feedback).toList();

        String expectedCompact = jamo(normalize(expectedText).replace(" ", ""));
        String actualCompact = jamo(normalize(transcription).replace(" ", ""));
        int accuracy = percent(similarity(expectedCompact, actualCompact));
        int completeness = completeness(expectedWords, alignment);
        int pace = paceScore(expectedCompact.length() / 3, durationSeconds);
        return new PronunciationAssessmentProvider.Assessment(
                PronunciationAttempt.Scores.builder()
                        .overall(accuracy).accuracy(accuracy).completeness(completeness).fluency(pace).build(),
                transcription == null ? "" : transcription.trim(), feedback, null);
    }

    /**
     * Rejects audio that clearly contains another sentence instead of turning
     * Azure's forced reference alignment into learner-facing pronunciation
     * feedback. WhisperX is the textual evidence; Azure completeness is only
     * used to confirm borderline transcript mismatches.
     */
    boolean isDifferentSentence(String expectedText, String transcription, Integer azureCompleteness) {
        String expected = normalize(expectedText);
        String actual = normalize(transcription);
        if (isClearlyDifferentSentence(expected, actual)) return true;

        int similarity = readingSimilarity(expected, actual);
        return similarity < PROBABLE_MISMATCH_SIMILARITY
                && azureCompleteness != null
                && azureCompleteness < LOW_AZURE_COMPLETENESS;
    }

    /**
     * The preflight gate deliberately uses only the unambiguous cases. It is
     * safe to run before Azure because it needs no Azure score: an empty
     * transcript or less than 35% Hangul similarity cannot be usefully
     * evaluated as the exercise sentence.
     */
    boolean isClearlyDifferentSentence(String expectedText, String transcription) {
        String expected = normalize(expectedText);
        String actual = normalize(transcription);
        if (expected.isBlank()) return false;
        if (actual.isBlank()) return true;
        return readingSimilarity(expected, actual) < CLEAR_MISMATCH_SIMILARITY;
    }

    /**
     * Learner-facing word coverage. A target word counts only when WhisperX
     * heard a sufficiently similar word in its aligned position; this avoids
     * treating a completely different substitution as an omitted word's match.
     */
    int completeness(String expectedText, String transcription) {
        List<String> expectedWords = words(expectedText);
        return completeness(expectedWords, align(expectedWords, words(transcription)));
    }

    int readingSimilarity(String expectedText, String transcription) {
        String expectedCompact = jamo(normalize(expectedText).replace(" ", ""));
        String actualCompact = jamo(normalize(transcription).replace(" ", ""));
        return percent(similarity(expectedCompact, actualCompact));
    }

    private List<WordMatch> align(List<String> expected, List<String> actual) {
        int rows = expected.size();
        int columns = actual.size();
        double[][] cost = new double[rows + 1][columns + 1];
        Move[][] moves = new Move[rows + 1][columns + 1];
        for (int row = 1; row <= rows; row++) { cost[row][0] = row; moves[row][0] = Move.DELETE; }
        for (int column = 1; column <= columns; column++) { cost[0][column] = column * INSERTION_COST; moves[0][column] = Move.INSERT; }
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= columns; column++) {
                double diagonal = cost[row - 1][column - 1] + (1d - similarity(jamo(expected.get(row - 1)), jamo(actual.get(column - 1))));
                double deletion = cost[row - 1][column] + 1d;
                double insertion = cost[row][column - 1] + INSERTION_COST;
                if (diagonal <= deletion && diagonal <= insertion) { cost[row][column] = diagonal; moves[row][column] = Move.MATCH; }
                else if (deletion <= insertion) { cost[row][column] = deletion; moves[row][column] = Move.DELETE; }
                else { cost[row][column] = insertion; moves[row][column] = Move.INSERT; }
            }
        }
        List<WordMatch> reversed = new ArrayList<>();
        int row = rows, column = columns;
        while (row > 0 || column > 0) {
            Move move = moves[row][column];
            if (move == Move.MATCH) {
                String target = expected.get(--row);
                String observed = actual.get(--column);
                reversed.add(new WordMatch(target, observed, percent(similarity(jamo(target), jamo(observed)))));
            } else if (move == Move.DELETE || column == 0) {
                reversed.add(new WordMatch(expected.get(--row), null, 0));
            } else {
                column--;
            }
        }
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private PronunciationAttempt.WordFeedback feedback(WordMatch match) {
        String detail = match.actual() == null
                ? "Không nhận dạng được từ này trong bản ghi."
                : "Whisper nhận dạng: " + match.actual() + " · độ khớp Hangul " + match.score() + "/100";
        return PronunciationAttempt.WordFeedback.builder().text(match.expected()).score(match.score())
                .accuracy(label(match.score())).phonemeDetail(detail).build();
    }

    private int completeness(List<String> expectedWords, List<WordMatch> alignment) {
        if (expectedWords.isEmpty()) return 0;
        long coveredWords = alignment.stream().filter(this::isCovered).count();
        return percent((double) coveredWords / expectedWords.size());
    }

    private boolean isCovered(WordMatch match) {
        return match.actual() != null && match.score() >= COVERED_WORD_SIMILARITY;
    }

    private List<String> words(String text) {
        String normalized = normalize(text);
        return normalized.isBlank() ? List.of() : Arrays.stream(normalized.split("\\s+")).filter(word -> !word.isBlank()).toList();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .replaceAll("[^\\p{IsHangul}\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    private String jamo(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0xAC00 || character > 0xD7A3) {
                result.append(character);
                continue;
            }
            int offset = character - 0xAC00;
            result.append((char) (0x1100 + offset / (21 * 28)));
            result.append((char) (0x1161 + (offset % (21 * 28)) / 28));
            int finalConsonant = offset % 28;
            if (finalConsonant > 0) result.append((char) (0x11A7 + finalConsonant));
        }
        return result.toString();
    }

    private double similarity(String left, String right) {
        if (left.equals(right)) return 1d;
        if (left.isEmpty() || right.isEmpty()) return 0d;
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitution = previous[column - 1] + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(Math.min(previous[column] + 1, current[column - 1] + 1), substitution);
            }
            int[] swap = previous; previous = current; current = swap;
        }
        return 1d - previous[right.length()] / (double) Math.max(left.length(), right.length());
    }

    private int paceScore(int syllables, double durationSeconds) {
        if (syllables == 0 || durationSeconds <= 0d) return 0;
        double syllablesPerSecond = syllables / durationSeconds;
        if (syllablesPerSecond >= 2d && syllablesPerSecond <= 6d) return 100;
        return Math.max(0, 100 - (int) Math.round(Math.abs(syllablesPerSecond - (syllablesPerSecond < 2d ? 2d : 6d)) * 25));
    }

    private int percent(double value) { return Math.max(0, Math.min(100, (int) Math.round(value * 100d))); }
    private String label(int score) { return score >= 85 ? "good" : score >= 60 ? "needs_practice" : "retry"; }

    private enum Move { MATCH, DELETE, INSERT }
    private record WordMatch(String expected, String actual, int score) { }
}

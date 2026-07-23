package com.kapor.video.service;

import com.kapor.video.model.Video;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lightweight, deterministic tokenizer for the subtitle editor.
 *
 * It intentionally does not claim to perform Korean morphological analysis; a
 * dedicated Korean NLP provider can replace this component without changing the
 * admin API.  Punctuation is discarded and Korean lexical units remain
 * clickable for dictionary lookup.
 */
@Component
public class SubtitleTokenizer {
    private static final Pattern TOKEN_BOUNDARY = Pattern.compile("[^\\p{IsHangul}A-Za-z0-9]+");

    public List<Video.TokenizedWord> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(TOKEN_BOUNDARY.split(text.trim()))
                .filter(token -> !token.isBlank())
                .map(token -> Video.TokenizedWord.builder()
                        .surface(token)
                        .stem(token.toLowerCase(Locale.ROOT))
                        .pos("unknown")
                        .clickable(token.codePoints().anyMatch(this::isHangul))
                        .build())
                .toList();
    }

    private boolean isHangul(int codePoint) {
        return (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
                || (codePoint >= 0x1100 && codePoint <= 0x11FF)
                || (codePoint >= 0x3130 && codePoint <= 0x318F);
    }
}

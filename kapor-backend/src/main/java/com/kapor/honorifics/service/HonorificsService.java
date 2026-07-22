package com.kapor.honorifics.service;

import com.kapor.honorifics.dto.CorrectionDiffDto;
import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic baseline for common business Korean transformations.
 * Keeping this server-side means a future Gemini structured-output provider can
 * enrich corrections without changing the mobile API contract.
 */
@Service
public class HonorificsService {

    private static final Map<String, Replacement> FORMAL_REPLACEMENTS = new LinkedHashMap<>();

    static {
        FORMAL_REPLACEMENTS.put("나", new Replacement("저", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        FORMAL_REPLACEMENTS.put("내가", new Replacement("제가", "particle", "Dùng '제가' thay cho '내가' trong câu trang trọng."));
        FORMAL_REPLACEMENTS.put("너", new Replacement("담당자", "pronoun", "Tránh '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        FORMAL_REPLACEMENTS.put("했어", new Replacement("하였습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        FORMAL_REPLACEMENTS.put("했어요", new Replacement("하였습니다", "verb_ending", "Dạng 하였습니다 phù hợp hơn với văn bản công sở trang trọng."));
        FORMAL_REPLACEMENTS.put("해봐", new Replacement("확인해 주시기 바랍니다", "verb_ending", "Chuyển mệnh lệnh thân mật thành yêu cầu lịch sự."));
        FORMAL_REPLACEMENTS.put("알려줘", new Replacement("알려 주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        FORMAL_REPLACEMENTS.put("고마워", new Replacement("감사합니다", "honorific", "Dùng '감사합니다' trong ngữ cảnh chuyên nghiệp."));
    }

    public HonorificsAnalysisDto analyze(String text, String targetLevel) {
        String source = text.trim();
        List<CorrectionDiffDto> corrections = new ArrayList<>();
        String transformed = source;
        for (Map.Entry<String, Replacement> entry : FORMAL_REPLACEMENTS.entrySet()) {
            if (transformed.contains(entry.getKey())) {
                Replacement replacement = entry.getValue();
                corrections.add(CorrectionDiffDto.builder()
                        .original(entry.getKey())
                        .corrected(replacement.value())
                        .type(replacement.type())
                        .explanation(replacement.explanation())
                        .build());
                transformed = transformed.replace(entry.getKey(), replacement.value());
            }
        }
        if ("hasipsio".equalsIgnoreCase(targetLevel) && transformed.endsWith(".") && !transformed.contains("습니다")) {
            transformed = transformed.replaceAll("(?<![.!?])$", ".");
        }
        String level = detectLevel(source);
        return HonorificsAnalysisDto.builder()
                .currentLevel(level)
                .confidence(corrections.isEmpty() ? 0.78 : 0.94)
                .corrections(corrections)
                .transformedText(transformed)
                .build();
    }

    public String transform(String text, String targetLevel) {
        return analyze(text, targetLevel).getTransformedText();
    }

    private String detectLevel(String text) {
        if (text.contains("습니다") || text.contains("드립니다") || text.contains("주시기 바랍니다")) return "hasipsio";
        if (text.contains("요")) return "heyohaet";
        return "banmal";
    }

    private record Replacement(String value, String type, String explanation) { }
}

package com.kapor.honorifics.service;

import com.kapor.honorifics.dto.CorrectionDiffDto;
import com.kapor.honorifics.dto.HonorificsAnalysisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Korean honorific analyser for the common business-writing
 * cases. The rules intentionally cover only transformations whose meaning is
 * stable without a full morphological parser; ambiguous wording is delegated
 * to {@link HonorificsAiService} only when it is a complete non-formal utterance.
 *
 * <p>The catalogue follows the three Korean honorific axes: subject
 * (주체 높임), object (객체/상대 높임) and addressee (상대 높임). This makes
 * the common path local, auditable and free of AI-token cost.</p>
 */
@Service
@RequiredArgsConstructor
public class HonorificsService {

    private static final String HASIPSIO = "hasipsio";
    private static final String HEYOHAET = "heyohaet";
    private static final String BANMAL = "banmal";

    /*
     * The list is deliberately explicit. Replacing every occurrence of -요 or
     * -시 with a regex is unsafe in Korean because conjugation depends on the
     * stem and on whether the person being discussed is the honoured subject.
     */
    private static final String HONORIFIC_TITLE =
            "(?:선생님|교수님|사장님|대표님|부장님|팀장님|과장님|차장님|이사님|전무님|상무님|고객님|담당자님"
                    + "|어르신|어머니|아버지|할머니|할아버지|부모님|선배님|의사 선생님)";
    private static final String BUSINESS_VERB_STEM =
            "(?:확인|검토|공유|진행|요청|회신|보고|연락|전달|제출|수정|적용|배포|설치|삭제|업데이트|등록|복구"
                    + "|승인|반영|준비|설정|테스트|안내|설명|답변)";

    private static final Pattern HONORIFIC_SUBJECT = Pattern.compile(
            HONORIFIC_TITLE + "(?:께서|께서는|은|는|이|가|\\s)");
    private static final Pattern HONORIFIC_RECIPIENT = Pattern.compile(
            HONORIFIC_TITLE + "(?:에게|한테|께)(?!서)");
    /*
     * A title followed by 이/가 is not automatically a subject: "팀장님이
     * 좋아요" means "I like the team lead" and must never become 께서. We
     * therefore recognise only predicates for which this catalogue has a
     * subject-honorific transformation.
     */
    private static final Pattern ACTION_PREDICATE = Pattern.compile(
            "(?:있었(?:어요|어)|있(?:습니다|어요|어)|먹었(?:어요|어)|먹(?:습니다|어요|어)"
                    + "|잤(?:어요|어)|자요|왔(?:어요|어)|와요|말했(?:어요|어)|말해(?:요)?"
                    + "|봤(?:어요|어)|봐요|아팠(?:어요|어)|아파(?:요)?|죽었(?:어요|어)"
                    + "|식사했(?:어요|어)|" + BUSINESS_VERB_STEM + "(?:했(?:어요|어)|합니다|해요))(?=[.!?]|\\s*$)");
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");
    private static final Pattern FORMAL_ENDING = Pattern.compile(
            "(?:습니다|습니까|십시오|ㅂ니다|ㅂ니까|입니다|입니까|겠습니다|랍니다)(?=[.!?]|\\s*$)");
    private static final Pattern HEYO_ENDING = Pattern.compile(
            "(?:아요|어요|예요|이에요|세요|네요|죠|군요|요)(?=[.!?]|\\s*$)");
    private static final Pattern CASUAL_ENDING = Pattern.compile(
            "(?:[가-힣](?:어|아|지|니|냐|자|라|네)|해봐|해|줘|거든|잖아)(?=[.!?]|\\s*$)");
    private static final Pattern SENTENCE = Pattern.compile("(?s)[^.!?]+(?:[.!?]+|$)");

    private static final List<Rule> SUBJECT_HONORIFIC_RULES = createSubjectHonorificRules();

    private static List<Rule> createSubjectHonorificRules() {
        List<Rule> rules = new ArrayList<>(List.of(
            terminalSuffix("식사했어요", "식사하셨습니다", "honorific", "Người được nhắc đến cần được tôn kính bằng '-시-'."),
            terminalSuffix("식사했어", "식사하셨습니다", "honorific", "Người được nhắc đến cần được tôn kính bằng '-시-'."),
            terminalSuffix("있었어요", "계셨습니다", "honorific", "Dùng '계시다' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("있었어", "계셨습니다", "honorific", "Dùng '계시다' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("있습니다", "계십니다", "honorific", "Dùng '계시다' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("있어요", "계십니다", "honorific", "Dùng '계시다' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("있어", "계십니다", "honorific", "Dùng '계시다' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("먹었습니다", "드셨습니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("먹었어요", "드셨습니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("먹었어", "드셨습니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("먹습니다", "드십니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("먹어요", "드십니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("먹어", "드십니다", "honorific", "Dùng '드시다' để tôn kính chủ thể khi nói về việc ăn."),
            terminalSuffix("잤어요", "주무셨습니다", "honorific", "Dùng '주무시다' để tôn kính chủ thể khi nói về việc ngủ."),
            terminalSuffix("잤어", "주무셨습니다", "honorific", "Dùng '주무시다' để tôn kính chủ thể khi nói về việc ngủ."),
            terminalSuffix("자요", "주무십니다", "honorific", "Dùng '주무시다' để tôn kính chủ thể khi nói về việc ngủ."),
            terminalSuffix("왔어요", "오셨습니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("왔어", "오셨습니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("와요", "오십니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("말했어요", "말씀하셨습니다", "honorific", "Dùng '말씀하시다' để tôn kính chủ thể khi truyền đạt lời nói."),
            terminalSuffix("말했어", "말씀하셨습니다", "honorific", "Dùng '말씀하시다' để tôn kính chủ thể khi truyền đạt lời nói."),
            terminalSuffix("말해요", "말씀하십니다", "honorific", "Dùng '말씀하시다' để tôn kính chủ thể khi truyền đạt lời nói."),
            terminalSuffix("말해", "말씀하십니다", "honorific", "Dùng '말씀하시다' để tôn kính chủ thể khi truyền đạt lời nói."),
            terminalSuffix("봤어요", "보셨습니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("봤어", "보셨습니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("봐요", "보십니다", "honorific", "Dùng '-시-' khi chủ thể là người cần được tôn kính."),
            terminalSuffix("아팠어요", "편찮으셨습니다", "honorific", "Dùng từ vựng kính ngữ khi nói về tình trạng của chủ thể."),
            terminalSuffix("아팠어", "편찮으셨습니다", "honorific", "Dùng từ vựng kính ngữ khi nói về tình trạng của chủ thể."),
            terminalSuffix("아파요", "편찮으십니다", "honorific", "Dùng từ vựng kính ngữ khi nói về tình trạng của chủ thể."),
            terminalSuffix("아파", "편찮으십니다", "honorific", "Dùng từ vựng kính ngữ khi nói về tình trạng của chủ thể."),
            terminalSuffix("죽었어요", "돌아가셨습니다", "honorific", "Dùng '돌아가시다' khi nhắc đến sự qua đời của người cần tôn kính."),
            terminalSuffix("죽었어", "돌아가셨습니다", "honorific", "Dùng '돌아가시다' khi nhắc đến sự qua đời của người cần tôn kính.")
        ));
        addSubjectBusinessVerbRules(rules, "확인");
        addSubjectBusinessVerbRules(rules, "검토");
        addSubjectBusinessVerbRules(rules, "공유");
        addSubjectBusinessVerbRules(rules, "진행");
        addSubjectBusinessVerbRules(rules, "요청");
        addSubjectBusinessVerbRules(rules, "회신");
        addSubjectBusinessVerbRules(rules, "보고");
        addSubjectBusinessVerbRules(rules, "연락");
        addSubjectBusinessVerbRules(rules, "전달");
        addSubjectBusinessVerbRules(rules, "제출");
        addSubjectBusinessVerbRules(rules, "수정");
        addSubjectBusinessVerbRules(rules, "적용");
        addSubjectBusinessVerbRules(rules, "배포");
        addSubjectBusinessVerbRules(rules, "설치");
        addSubjectBusinessVerbRules(rules, "삭제");
        addSubjectBusinessVerbRules(rules, "업데이트");
        addSubjectBusinessVerbRules(rules, "등록");
        addSubjectBusinessVerbRules(rules, "복구");
        addSubjectBusinessVerbRules(rules, "승인");
        addSubjectBusinessVerbRules(rules, "반영");
        addSubjectBusinessVerbRules(rules, "준비");
        addSubjectBusinessVerbRules(rules, "설정");
        addSubjectBusinessVerbRules(rules, "테스트");
        addSubjectBusinessVerbRules(rules, "안내");
        addSubjectBusinessVerbRules(rules, "설명");
        addSubjectBusinessVerbRules(rules, "답변");
        return List.copyOf(rules);
    }

    private static final List<Rule> OBJECT_HONORIFIC_RULES = List.of(
            terminalSuffix("주었어요", "드렸습니다", "honorific", "Dùng '드리다' khi hành động hướng tới người nhận cần được tôn kính."),
            terminalSuffix("줬어요", "드렸습니다", "honorific", "Dùng '드리다' khi hành động hướng tới người nhận cần được tôn kính."),
            terminalSuffix("줬어", "드렸습니다", "honorific", "Dùng '드리다' khi hành động hướng tới người nhận cần được tôn kính."),
            terminalSuffix("줍니다", "드립니다", "honorific", "Dùng '드리다' khi hành động hướng tới người nhận cần được tôn kính."),
            terminalSuffix("줘", "드리십시오", "honorific", "Dùng '드리다' khi hành động hướng tới người nhận cần được tôn kính."),
            terminalSuffix("물어봤어요", "여쭈었습니다", "honorific", "Dùng '여쭙다' khi hỏi người cần được tôn kính."),
            terminalSuffix("물어봤어", "여쭈었습니다", "honorific", "Dùng '여쭙다' khi hỏi người cần được tôn kính."),
            terminalSuffix("물어봐", "여쭤 보십시오", "honorific", "Dùng '여쭙다' khi hỏi người cần được tôn kính.")
    );

    private static final List<Rule> PARTICLE_RULES = List.of(
            regex("(" + HONORIFIC_TITLE + ")(?:이|가)", match -> match.group(1) + "께서", "particle",
                    "Dùng '께서' cho chủ thể là người cần được tôn kính."),
            regex("(" + HONORIFIC_TITLE + ")(?:에게|한테)", match -> match.group(1) + "께", "particle",
                    "Dùng '께' thay cho '에게/한테' với người nhận cần được tôn kính.")
    );

    private static final List<Rule> FORMAL_RULES = createFormalRules();

    private final HonorificsAiService honorificsAiService;

    public HonorificsAnalysisDto analyze(String text, String targetLevel) {
        String source = text.trim();
        String normalizedTarget = normalizeTargetLevel(targetLevel);
        List<CorrectionDiffDto> corrections = new ArrayList<>();
        String transformed = HASIPSIO.equals(normalizedTarget)
                ? transformToHasipsio(source, corrections)
                : source;
        String currentLevel = detectLevel(source);

        HonorificsAnalysisDto ruleBasedResult = HonorificsAnalysisDto.builder()
                .currentLevel(currentLevel)
                .confidence(ruleConfidence(source, transformed, corrections))
                .analysisSource("rule_based")
                .corrections(corrections)
                .transformedText(transformed)
                .build();

        // Do not send text that is already in the requested style to Gemini.
        // AI is reserved for complete casual/polite utterances outside this
        // conservative catalogue, where sentence-level context is necessary.
        return shouldUseAiFallback(source, normalizedTarget, currentLevel, corrections)
                ? honorificsAiService.analyze(source, normalizedTarget).orElse(ruleBasedResult)
                : ruleBasedResult;
    }

    public String transform(String text, String targetLevel) {
        return analyze(text, targetLevel).getTransformedText();
    }

    private String transformToHasipsio(String source, List<CorrectionDiffDto> corrections) {
        Matcher matcher = SENTENCE.matcher(source);
        StringBuffer transformed = new StringBuffer();
        while (matcher.find()) {
            String sentence = matcher.group();
            String converted = sentence;
            boolean hasHonorificSubject = hasHonorificSubject(sentence);
            boolean hasHonorificRecipient = HONORIFIC_RECIPIENT.matcher(sentence).find();

            if (hasHonorificSubject) {
                converted = applyRules(converted, corrections, SUBJECT_HONORIFIC_RULES);
                converted = applyRules(converted, corrections, PARTICLE_RULES.subList(0, 1));
            }
            if (hasHonorificRecipient) {
                converted = applyRules(converted, corrections, OBJECT_HONORIFIC_RULES);
                converted = applyRules(converted, corrections, PARTICLE_RULES.subList(1, 2));
            }
            converted = applyRules(converted, corrections, FORMAL_RULES);
            matcher.appendReplacement(transformed, Matcher.quoteReplacement(converted));
        }
        matcher.appendTail(transformed);
        return transformed.toString();
    }

    private boolean hasHonorificSubject(String sentence) {
        return HONORIFIC_SUBJECT.matcher(sentence).find() && ACTION_PREDICATE.matcher(sentence).find();
    }

    private String applyRules(String source, List<CorrectionDiffDto> corrections, List<Rule> rules) {
        String transformed = source;
        for (Rule rule : rules) {
            transformed = rule.apply(transformed, corrections);
        }
        return transformed;
    }

    private boolean shouldUseAiFallback(
            String source,
            String targetLevel,
            String currentLevel,
            List<CorrectionDiffDto> corrections) {
        if (!corrections.isEmpty() || !HANGUL.matcher(source).find()) return false;
        if (targetLevel.equals(currentLevel)) return false;
        if (HASIPSIO.equals(targetLevel) && HASIPSIO.equals(currentLevel)) return false;
        return FORMAL_ENDING.matcher(source).find()
                || HEYO_ENDING.matcher(source).find()
                || CASUAL_ENDING.matcher(source).find();
    }

    private double ruleConfidence(String source, String transformed, List<CorrectionDiffDto> corrections) {
        if (!corrections.isEmpty()) return 0.97;
        if (HASIPSIO.equals(detectLevel(source))) return 0.96;
        return source.equals(transformed) ? 0.82 : 0.90;
    }

    private String detectLevel(String text) {
        if (CASUAL_ENDING.matcher(text).find()) return BANMAL;
        if (HEYO_ENDING.matcher(text).find()) return HEYOHAET;
        if (FORMAL_ENDING.matcher(text).find()) return HASIPSIO;
        return BANMAL;
    }

    private String normalizeTargetLevel(String targetLevel) {
        if (targetLevel == null || targetLevel.isBlank()) return HASIPSIO;
        String normalized = targetLevel.trim().toLowerCase(Locale.ROOT);
        return List.of(HASIPSIO, HEYOHAET, BANMAL).contains(normalized) ? normalized : HASIPSIO;
    }

    private static List<Rule> createFormalRules() {
        List<Rule> rules = new ArrayList<>();

        // Request phrases must run before short terminal endings such as 확인해.
        rules.add(regex("해\\s*주세요", ignored -> "해 주십시오", "verb_ending",
                "Đổi lời đề nghị 해요체 sang mệnh lệnh lịch sự 하십시오체."));
        rules.add(regex("해\\s*줘", ignored -> "해 주십시오", "verb_ending",
                "Đổi lời đề nghị thân mật sang mệnh lệnh lịch sự 하십시오체."));
        rules.add(regex("해\\s*보세요", ignored -> "해 보십시오", "verb_ending",
                "Đổi đề nghị 해요체 sang hạ십시오체."));
        rules.add(regex("해\\s*봐", ignored -> "해 보십시오", "verb_ending",
                "Đổi đề nghị thân mật sang hạ십시오체."));
        rules.add(regex("(?<=[가-힣])\\s*주세요", ignored -> " 주십시오", "verb_ending",
                "Đổi lời đề nghị 해요체 sang mệnh lệnh lịch sự 하십시오체."));
        rules.add(regex("(?<=[가-힣])\\s*줘", ignored -> " 주십시오", "verb_ending",
                "Đổi lời đề nghị thân mật sang mệnh lệnh lịch sự 하십시오체."));

        // Humble first-person expressions and neutral professional addressee.
        rules.add(word("나에게", "저에게", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        rules.add(word("나한테", "저에게", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        rules.add(word("나를", "저를", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        rules.add(word("나는", "저는", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        rules.add(word("내가", "제가", "pronoun", "Dùng '제' thay cho '내' trong câu trang trọng."));
        rules.add(word("내게", "제게", "pronoun", "Dùng '제' thay cho '내' trong câu trang trọng."));
        rules.add(word("내", "제", "pronoun", "Dùng '제' thay cho '내' trong câu trang trọng."));
        rules.add(word("나", "저", "pronoun", "Dùng '저' để xưng hô khiêm nhường trong ngữ cảnh công việc."));
        rules.add(word("우리", "저희", "pronoun", "Dùng '저희' cho tổ chức/nhóm của người nói trong ngữ cảnh trang trọng."));
        rules.add(word("너에게", "담당자에게", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        rules.add(word("너한테", "담당자에게", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        rules.add(word("너를", "담당자를", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        rules.add(word("너는", "담당자는", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        rules.add(word("네가", "담당자가", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));
        rules.add(word("너", "담당자", "pronoun", "Tránh xưng hô trực tiếp bằng '너' trong công việc; dùng vai trò hoặc tên người nhận."));

        // High-frequency business salutations, apologies and requests.
        rules.add(terminal("안녕하세요", "안녕하십니까", "verb_ending", "Dùng lời chào 하십시오체 trong bối cảnh công việc trang trọng."));
        rules.add(terminal("안녕", "안녕하십니까", "verb_ending", "Dùng lời chào 하십시오체 trong bối cảnh công việc trang trọng."));
        rules.add(terminal("고마워요", "감사합니다", "honorific", "Dùng '감사합니다' trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("고마워", "감사합니다", "honorific", "Dùng '감사합니다' trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("감사해요", "감사합니다", "honorific", "Dùng '감사합니다' trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("감사해", "감사합니다", "honorific", "Dùng '감사합니다' trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("미안해요", "죄송합니다", "honorific", "Dùng '죄송합니다' để xin lỗi trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("미안해", "죄송합니다", "honorific", "Dùng '죄송합니다' để xin lỗi trong ngữ cảnh chuyên nghiệp."));
        rules.add(terminal("부탁해요", "부탁드립니다", "honorific", "Dùng '부탁드립니다' cho lời đề nghị công việc lịch sự."));
        rules.add(terminal("부탁해", "부탁드립니다", "honorific", "Dùng '부탁드립니다' cho lời đề nghị công việc lịch sự."));
        rules.add(terminal("알려줘", "알려 주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        rules.add(terminal("알려줘요", "알려 주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        rules.add(terminal("보내줘", "보내 주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        rules.add(terminal("도와줘", "도와주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        rules.add(terminal("기다려줘", "기다려 주시기 바랍니다", "verb_ending", "Dùng mẫu yêu cầu lịch sự trong giao tiếp công việc."));
        rules.add(terminal("알겠어요", "알겠습니다", "verb_ending", "Dùng '알겠습니다' trong giao tiếp công việc trang trọng."));
        rules.add(terminal("알겠어", "알겠습니다", "verb_ending", "Dùng '알겠습니다' trong giao tiếp công việc trang trọng."));
        rules.add(terminal("네", "예", "honorific", "Dùng '예' trong phản hồi trang trọng."));
        rules.add(terminal("응", "예", "honorific", "Dùng '예' trong phản hồi trang trọng."));

        // Regular high-frequency predicates that have a stable 하십시오체 form.
        rules.add(terminalSuffix("했어요", "했습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("했어", "했습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("할게요", "하겠습니다", "verb_ending", "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));
        rules.add(terminalSuffix("할게", "하겠습니다", "verb_ending", "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));
        rules.add(terminalSuffix("있었어요", "있었습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("있었어", "있었습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("있어요", "있습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("있어", "있습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("없었어요", "없었습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("없었어", "없었습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("없어요", "없습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("없어", "없습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("좋아요", "좋습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("좋아", "좋습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("같아요", "같습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("같아", "같습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("가능해요", "가능합니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("가능해", "가능합니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("필요해요", "필요합니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("필요해", "필요합니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("괜찮아요", "괜찮습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("괜찮아", "괜찮습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("맞아요", "맞습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("맞아", "맞습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("아니에요", "아닙니다", "verb_ending", "Dùng '아닙니다' trong câu trang trọng."));
        rules.add(terminalSuffix("아니야", "아닙니다", "verb_ending", "Dùng '아닙니다' trong câu trang trọng."));
        rules.add(terminalSuffix("됐어요", "되었습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("됐어", "되었습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("끝났어요", "끝났습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("끝났어", "끝났습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));

        addBusinessVerbRules(rules, "확인");
        addBusinessVerbRules(rules, "검토");
        addBusinessVerbRules(rules, "공유");
        addBusinessVerbRules(rules, "진행");
        addBusinessVerbRules(rules, "요청");
        addBusinessVerbRules(rules, "회신");
        addBusinessVerbRules(rules, "보고");
        addBusinessVerbRules(rules, "연락");
        addBusinessVerbRules(rules, "전달");
        addBusinessVerbRules(rules, "제출");
        addBusinessVerbRules(rules, "수정");
        addBusinessVerbRules(rules, "적용");
        addBusinessVerbRules(rules, "배포");
        addBusinessVerbRules(rules, "설치");
        addBusinessVerbRules(rules, "삭제");
        addBusinessVerbRules(rules, "업데이트");
        addBusinessVerbRules(rules, "등록");
        addBusinessVerbRules(rules, "복구");
        addBusinessVerbRules(rules, "승인");
        addBusinessVerbRules(rules, "반영");
        addBusinessVerbRules(rules, "준비");
        addBusinessVerbRules(rules, "설정");
        addBusinessVerbRules(rules, "테스트");
        addBusinessVerbRules(rules, "안내");
        addBusinessVerbRules(rules, "설명");
        addBusinessVerbRules(rules, "답변");

        rules.add(terminalSuffix("보냈어요", "보냈습니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("보냈어", "보냈습니다", "verb_ending", "Đổi đuôi 반말 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("보내요", "보냅니다", "verb_ending", "Đổi đuôi 해요체 sang dạng trang trọng 하십시오체."));
        rules.add(terminalSuffix("보내", "보내십시오", "verb_ending", "Đổi yêu cầu thân mật sang mệnh lệnh lịch sự 하십시오체."));
        rules.add(terminalSuffix("보낼게요", "보내겠습니다", "verb_ending", "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));
        rules.add(terminalSuffix("보낼게", "보내겠습니다", "verb_ending", "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));

        return List.copyOf(rules);
    }

    private static void addBusinessVerbRules(List<Rule> rules, String stem) {
        String explanation = "Đổi đuôi câu sang dạng trang trọng 하십시오체.";
        rules.add(terminalSuffix(stem + "했어요", stem + "했습니다", "verb_ending", explanation));
        rules.add(terminalSuffix(stem + "했어", stem + "했습니다", "verb_ending", explanation));
        rules.add(terminalSuffix(stem + "해요", stem + "합니다", "verb_ending", explanation));
        rules.add(terminalSuffix(stem + "해", stem + "하십시오", "verb_ending", explanation));
        rules.add(terminalSuffix(stem + "할게요", stem + "하겠습니다", "verb_ending",
                "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));
        rules.add(terminalSuffix(stem + "할게", stem + "하겠습니다", "verb_ending",
                "Dùng '-겠습니다' để biểu thị cam kết trong ngữ cảnh công việc."));
    }

    private static void addSubjectBusinessVerbRules(List<Rule> rules, String stem) {
        String explanation = "Thêm '-시-' vì chủ thể là người cần được tôn kính.";
        rules.add(terminalSuffix(stem + "했습니다", stem + "하셨습니다", "honorific", explanation));
        rules.add(terminalSuffix(stem + "했어요", stem + "하셨습니다", "honorific", explanation));
        rules.add(terminalSuffix(stem + "했어", stem + "하셨습니다", "honorific", explanation));
        rules.add(terminalSuffix(stem + "합니다", stem + "하십니다", "honorific", explanation));
        rules.add(terminalSuffix(stem + "해요", stem + "하십니다", "honorific", explanation));
    }

    private static Rule word(String original, String corrected, String type, String explanation) {
        return regex("(?<![가-힣])" + Pattern.quote(original) + "(?![가-힣])", ignored -> corrected, type, explanation);
    }

    private static Rule terminal(String original, String corrected, String type, String explanation) {
        return regex("(?<![가-힣])" + Pattern.quote(original) + "(?=[.!?\\s]*$)", ignored -> corrected, type, explanation);
    }

    private static Rule terminalSuffix(String original, String corrected, String type, String explanation) {
        return regex(Pattern.quote(original) + "(?=[.!?\\s]*$)", ignored -> corrected, type, explanation);
    }

    private static Rule regex(String expression, Function<Matcher, String> replacement, String type, String explanation) {
        return new Rule(Pattern.compile(expression), replacement, type, explanation);
    }

    private record Rule(
            Pattern pattern,
            Function<Matcher, String> replacement,
            String type,
            String explanation) {

        private String apply(String source, List<CorrectionDiffDto> corrections) {
            Matcher matcher = pattern.matcher(source);
            StringBuffer transformed = new StringBuffer();
            boolean matched = false;
            while (matcher.find()) {
                matched = true;
                String original = matcher.group();
                String corrected = replacement.apply(matcher);
                corrections.add(CorrectionDiffDto.builder()
                        .original(original)
                        .corrected(corrected)
                        .type(type)
                        .explanation(explanation)
                        .build());
                matcher.appendReplacement(transformed, Matcher.quoteReplacement(corrected));
            }
            if (!matched) return source;
            matcher.appendTail(transformed);
            return transformed.toString();
        }
    }
}

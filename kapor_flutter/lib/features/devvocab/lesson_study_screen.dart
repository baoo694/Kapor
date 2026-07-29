import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/audio/korean_tts_player.dart';
import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';

class LessonStudyScreen extends StatefulWidget {
  final String lessonId;
  final DevVocabLesson? initialLesson;

  const LessonStudyScreen({
    super.key,
    required this.lessonId,
    this.initialLesson,
  });

  @override
  State<LessonStudyScreen> createState() => _LessonStudyScreenState();
}

class _LessonStudyScreenState extends State<LessonStudyScreen> {
  final _service = DevVocabService();
  DevVocabLesson? _lesson;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _lesson = widget.initialLesson;
    _load();
  }

  Future<void> _load() async {
    try {
      final lesson = await _service.getLesson(widget.lessonId);
      if (!mounted) return;
      setState(() {
        _lesson = lesson;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<void> _completeStudy() async {
    setState(() => _saving = true);
    try {
      await _service.completeStudy(widget.lessonId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã hoàn thành phần Học. Mở khóa Kiểm tra!'),
        ),
      );
      context.pop(true);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final lesson = _lesson;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Học'),
        backgroundColor: const Color(0xFF102126),
      ),
      body: _loading && lesson == null
          ? const Center(child: CircularProgressIndicator())
          : lesson == null
          ? _ErrorState(
              message: _error ?? 'Không thể tải nội dung.',
              onRetry: _load,
            )
          : SafeArea(
              top: false,
              child: Column(
                children: [
                  Expanded(
                    child: ListView(
                      padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
                      children: [
                        _Eyebrow(text: 'BÀI HỌC ${lesson.order + 1}'),
                        const SizedBox(height: 8),
                        Text(
                          lesson.titleVi,
                          style: GoogleFonts.outfit(
                            color: Colors.white,
                            fontSize: 25,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 20),
                        _BilingualLessonContent(
                          koreanContent: lesson.content,
                          vietnameseContent: lesson.contentVi,
                        ),
                        const SizedBox(height: 24),
                        _VocabularySection(vocabulary: lesson.vocabulary),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
                    child: SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _saving ? null : _completeStudy,
                        icon: _saving
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                ),
                              )
                            : const Icon(Icons.check_rounded),
                        label: Text(_saving ? 'Đang lưu...' : 'Đã học xong'),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _BilingualLessonContent extends StatelessWidget {
  final String koreanContent;
  final String vietnameseContent;

  const _BilingualLessonContent({
    required this.koreanContent,
    required this.vietnameseContent,
  });

  @override
  Widget build(BuildContext context) {
    final koreanSections = _parseSections(koreanContent);
    final vietnameseSections = _parseSections(vietnameseContent);
    final sectionCount = koreanSections.length > vietnameseSections.length
        ? koreanSections.length
        : vietnameseSections.length;
    if (sectionCount == 0) {
      return const _StudyMessage(
        message: 'Nội dung bài học đang được cập nhật.',
      );
    }

    return Column(
      children: List.generate(sectionCount, (index) {
        final korean = index < koreanSections.length
            ? koreanSections[index]
            : const _StudySection();
        final vietnamese = index < vietnameseSections.length
            ? vietnameseSections[index]
            : const _StudySection();
        return Padding(
          padding: EdgeInsets.only(bottom: index == sectionCount - 1 ? 0 : 14),
          child: _BilingualSectionCard(korean: korean, vietnamese: vietnamese),
        );
      }),
    );
  }
}

class _BilingualSectionCard extends StatelessWidget {
  final _StudySection korean;
  final _StudySection vietnamese;

  const _BilingualSectionCard({required this.korean, required this.vietnamese});

  @override
  Widget build(BuildContext context) {
    final koreanTitle = _hasHangul(korean.title)
        ? korean.title
        : vietnamese.title;
    final vietnameseTitle = _hasHangul(korean.title)
        ? vietnamese.title
        : korean.title;
    final koreanBody = _hasHangul(korean.body) ? korean.body : vietnamese.body;
    final vietnameseBody = _hasHangul(korean.body)
        ? vietnamese.body
        : korean.body;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF18213B),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: const Color(0xFF6B82FF).withValues(alpha: .32),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            koreanTitle,
            style: GoogleFonts.outfit(
              color: Colors.white,
              fontSize: 19,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (vietnameseTitle.isNotEmpty) ...[
            const SizedBox(height: 2),
            Text(
              vietnameseTitle,
              style: GoogleFonts.inter(
                color: AppTheme.primary,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
          const SizedBox(height: 14),
          _LanguageContent(
            label: '한국어',
            content: koreanBody,
            color: const Color(0xFFE7E9F4),
          ),
          if (koreanBody.isNotEmpty && vietnameseBody.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 14),
              child: Divider(
                color: Colors.white.withValues(alpha: .09),
                height: 1,
              ),
            ),
          _LanguageContent(
            label: 'TIẾNG VIỆT',
            content: vietnameseBody,
            color: const Color(0xFFBFC8E3),
          ),
        ],
      ),
    );
  }
}

class _LanguageContent extends StatelessWidget {
  final String label;
  final String content;
  final Color color;

  const _LanguageContent({
    required this.label,
    required this.content,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    if (content.isEmpty) return const SizedBox.shrink();
    final lines = content.split('\n').where((line) => line.trim().isNotEmpty);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: GoogleFonts.jetBrainsMono(
            color: AppTheme.textSecondary,
            fontSize: 10,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 6),
        ...lines.map(
          (line) => Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Text(
              line.replaceFirst(RegExp(r'^\d+\.\s*'), '• '),
              style: GoogleFonts.inter(color: color, height: 1.6, fontSize: 15),
            ),
          ),
        ),
      ],
    );
  }
}

class _StudyMessage extends StatelessWidget {
  final String message;
  const _StudyMessage({required this.message});
  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      color: const Color(0xFF18213B),
      borderRadius: BorderRadius.circular(20),
    ),
    child: Text(message, style: const TextStyle(color: AppTheme.textSecondary)),
  );
}

class _StudySection {
  final String title;
  final String body;
  const _StudySection({this.title = '', this.body = ''});
}

List<_StudySection> _parseSections(String markdown) {
  if (markdown.trim().isEmpty) return const [];
  final sections = <_StudySection>[];
  String title = '';
  final body = <String>[];

  void saveSection() {
    if (title.isNotEmpty || body.isNotEmpty) {
      sections.add(_StudySection(title: title, body: body.join('\n').trim()));
    }
  }

  for (final line in markdown.split('\n')) {
    final header = RegExp(r'^\s{0,3}#{1,6}\s+(.+?)\s*$').firstMatch(line);
    if (header != null) {
      saveSection();
      title = header.group(1) ?? '';
      body.clear();
    } else {
      body.add(line);
    }
  }
  saveSection();
  return sections;
}

bool _hasHangul(String value) => RegExp(r'[\uAC00-\uD7AF]').hasMatch(value);

class _VocabularySection extends StatelessWidget {
  final List<LessonVocabularyItem> vocabulary;
  const _VocabularySection({required this.vocabulary});

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        'Từ vựng trọng tâm',
        style: GoogleFonts.outfit(
          color: Colors.white,
          fontSize: 19,
          fontWeight: FontWeight.w700,
        ),
      ),
      const SizedBox(height: 10),
      ...vocabulary.map(
        (word) => Container(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Expanded(
                flex: 5,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      word.korean,
                      style: GoogleFonts.outfit(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    if (word.pronunciation.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        '/${word.pronunciation}/',
                        style: GoogleFonts.jetBrainsMono(
                          color: const Color(0xFFAEB8D9),
                          fontSize: 10,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              _VocabularyPronunciationButton(korean: word.korean),
              const SizedBox(width: 8),
              Expanded(
                flex: 5,
                child: Text(
                  word.vietnamese,
                  textAlign: TextAlign.right,
                  style: GoogleFonts.inter(
                    color: AppTheme.textSecondary,
                    fontSize: 13,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    ],
  );
}

class _VocabularyPronunciationButton extends StatelessWidget {
  final String korean;

  const _VocabularyPronunciationButton({required this.korean});

  Future<void> _play(BuildContext context) async {
    try {
      await KoreanTtsPlayer.instance.playOrStop(korean);
    } on KoreanTtsException catch (error) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  @override
  Widget build(BuildContext context) => ValueListenableBuilder<String?>(
    valueListenable: KoreanTtsPlayer.instance.activeText,
    builder: (context, activeText, child) {
      final isPlaying = activeText == korean;
      return Semantics(
        button: true,
        label: isPlaying ? 'Dừng phát âm $korean' : 'Nghe phát âm $korean',
        child: IconButton(
          tooltip: isPlaying ? 'Dừng phát âm' : 'Nghe phát âm',
          onPressed: () => _play(context),
          style: IconButton.styleFrom(
            backgroundColor: AppTheme.primary.withValues(alpha: .12),
            foregroundColor: AppTheme.primary,
          ),
          icon: isPlaying
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.volume_up_outlined, size: 20),
        ),
      );
    },
  );
}

class _Eyebrow extends StatelessWidget {
  final String text;
  const _Eyebrow({required this.text});
  @override
  Widget build(BuildContext context) => Text(
    text,
    style: GoogleFonts.jetBrainsMono(
      color: AppTheme.primary,
      fontWeight: FontWeight.w700,
      fontSize: 11,
    ),
  );
}

class _ErrorState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;
  const _ErrorState({required this.message, required this.onRetry});
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: AppTheme.textSecondary),
          const SizedBox(height: 12),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppTheme.textSecondary),
          ),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    ),
  );
}

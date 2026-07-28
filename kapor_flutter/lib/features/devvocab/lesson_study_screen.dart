import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

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
                        _ContentCard(
                          content: lesson.contentVi.isNotEmpty
                              ? lesson.contentVi
                              : lesson.content,
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

class _ContentCard extends StatelessWidget {
  final String content;
  const _ContentCard({required this.content});

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      color: const Color(0xFF18213B),
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: const Color(0xFF6B82FF).withValues(alpha: .32)),
    ),
    child: Text(
      content.isEmpty ? 'Nội dung bài học đang được cập nhật.' : content,
      style: GoogleFonts.inter(
        color: const Color(0xFFE5E8F5),
        height: 1.7,
        fontSize: 15,
      ),
    ),
  );
}

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
            children: [
              Expanded(
                child: Text(
                  word.korean,
                  style: GoogleFonts.outfit(
                    color: Colors.white,
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Text(
                word.vietnamese,
                style: GoogleFonts.inter(color: AppTheme.textSecondary),
              ),
            ],
          ),
        ),
      ),
    ],
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

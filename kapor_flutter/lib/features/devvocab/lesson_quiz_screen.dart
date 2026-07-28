import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';

class LessonQuizScreen extends StatefulWidget {
  final String lessonId;
  final DevVocabLesson? initialLesson;

  const LessonQuizScreen({
    super.key,
    required this.lessonId,
    this.initialLesson,
  });

  @override
  State<LessonQuizScreen> createState() => _LessonQuizScreenState();
}

class _LessonQuizScreenState extends State<LessonQuizScreen> {
  final _service = DevVocabService();
  final Map<String, String> _answers = {};
  DevVocabLesson? _lesson;
  int _index = 0;
  bool _loading = true;
  bool _submitting = false;
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

  Future<void> _submit() async {
    final lesson = _lesson!;
    if (_answers.length != lesson.exercises.length) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Hãy trả lời tất cả câu hỏi.')),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      final result = await _service.submitQuiz(
        lessonId: lesson.id,
        answers: _answers,
      );
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: const Color(0xFF202945),
          title: Text(
            result.passed ? 'Bạn đã đạt!' : 'Chưa đạt',
            style: const TextStyle(color: Colors.white),
          ),
          content: Text(
            '${result.correctAnswers}/${result.totalQuestions} câu đúng · ${result.score}%\n${result.passed ? 'Lesson đã hoàn thành.' : 'Cần từ 80% để hoàn thành. Bạn có thể làm lại.'}',
            style: const TextStyle(color: Color(0xFFD5D9E9), height: 1.5),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: Text(result.passed ? 'Xong' : 'Làm lại'),
            ),
          ],
        ),
      );
      if (!mounted) return;
      if (result.passed) {
        context.pop(true);
      } else {
        setState(() {
          _index = 0;
          _answers.clear();
        });
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final lesson = _lesson;
    if (_loading && lesson == null) {
      return const Scaffold(
        backgroundColor: AppTheme.background,
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (lesson == null || lesson.exercises.isEmpty) {
      return Scaffold(
        backgroundColor: AppTheme.background,
        appBar: AppBar(title: const Text('Kiểm tra')),
        body: Center(
          child: Text(
            _error ?? 'Bài học này chưa có câu hỏi.',
            style: const TextStyle(color: AppTheme.textSecondary),
          ),
        ),
      );
    }
    final exercise = lesson.exercises[_index];
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Kiểm tra'),
        backgroundColor: const Color(0xFF102126),
      ),
      body: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'CÂU ${_index + 1}/${lesson.exercises.length}',
                style: GoogleFonts.jetBrainsMono(
                  color: AppTheme.primary,
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 10),
              ClipRRect(
                borderRadius: BorderRadius.circular(99),
                child: LinearProgressIndicator(
                  value: (_index + 1) / lesson.exercises.length,
                  minHeight: 6,
                ),
              ),
              const SizedBox(height: 28),
              Text(
                exercise.questionVi.isNotEmpty
                    ? exercise.questionVi
                    : exercise.question,
                style: GoogleFonts.outfit(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  height: 1.25,
                ),
              ),
              const SizedBox(height: 24),
              Expanded(
                child: ListView.separated(
                  itemCount: exercise.options.length,
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: 10),
                  itemBuilder: (context, optionIndex) {
                    final option = exercise.options[optionIndex];
                    final selected = _answers[exercise.id] == option;
                    return Semantics(
                      selected: selected,
                      button: true,
                      label: 'Đáp án ${optionIndex + 1}: $option',
                      child: InkWell(
                        onTap: () =>
                            setState(() => _answers[exercise.id] = option),
                        borderRadius: BorderRadius.circular(16),
                        child: Ink(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: selected
                                ? const Color(0xFF314879)
                                : AppTheme.surface,
                            border: Border.all(
                              color: selected
                                  ? AppTheme.primary
                                  : Colors.white.withValues(alpha: .09),
                            ),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 28,
                                height: 28,
                                alignment: Alignment.center,
                                decoration: BoxDecoration(
                                  color: selected
                                      ? AppTheme.primary
                                      : Colors.white.withValues(alpha: .08),
                                  shape: BoxShape.circle,
                                ),
                                child: Text(
                                  String.fromCharCode(65 + optionIndex),
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 13),
                              Expanded(
                                child: Text(
                                  option,
                                  style: GoogleFonts.inter(
                                    color: Colors.white,
                                    fontSize: 15,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _submitting
                      ? null
                      : _index == lesson.exercises.length - 1
                      ? _submit
                      : _answers.containsKey(exercise.id)
                      ? () => setState(() => _index++)
                      : null,
                  child: Text(
                    _submitting
                        ? 'Đang chấm...'
                        : _index == lesson.exercises.length - 1
                        ? 'Nộp bài'
                        : 'Câu tiếp theo',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

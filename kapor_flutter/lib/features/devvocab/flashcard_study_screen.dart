import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';
import 'flashcard_summary_screen.dart';
import 'widgets/vocabulary_flip_card.dart';

class FlashcardStudyScreen extends StatefulWidget {
  final String lessonId;
  final DevVocabLesson? initialLesson;

  const FlashcardStudyScreen({
    super.key,
    required this.lessonId,
    this.initialLesson,
  });

  @override
  State<FlashcardStudyScreen> createState() => _FlashcardStudyScreenState();
}

class _FlashcardStudyScreenState extends State<FlashcardStudyScreen> {
  final DevVocabService _devVocabService = DevVocabService();
  DevVocabLesson? _lesson;
  FlashcardProgress? _progress;
  bool _isLoading = true;
  bool _isSaving = false;
  String? _errorMessage;
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _lesson = widget.initialLesson;
    _loadSession();
  }

  Future<void> _loadSession() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final results = await Future.wait([
        _devVocabService.getLesson(widget.lessonId),
        _devVocabService.getFlashcardProgress(widget.lessonId),
      ]);
      if (!mounted) {
        return;
      }
      setState(() {
        _lesson = results[0] as DevVocabLesson;
        _progress = results[1] as FlashcardProgress;
        _currentIndex = 0;
        _isLoading = false;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _isLoading = false;
        _errorMessage = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<bool> _confirmDismiss(DismissDirection direction) async {
    if (_isSaving) {
      return false;
    }
    final lesson = _lesson;
    if (lesson == null || _currentIndex >= lesson.vocabulary.length) {
      return false;
    }
    final vocabulary = lesson.vocabulary[_currentIndex];
    if (vocabulary.id.isEmpty) {
      _showError('Thẻ này chưa có ID. Vui lòng lưu lại Lesson trong Admin.');
      return false;
    }

    final status = direction == DismissDirection.startToEnd
        ? 'KNOWN'
        : 'LEARNING';
    setState(() => _isSaving = true);
    try {
      final progress = await _devVocabService.updateFlashcardStatus(
        lessonId: lesson.id,
        vocabularyId: vocabulary.id,
        status: status,
      );
      if (!mounted) {
        return false;
      }
      setState(() => _progress = progress);
      return true;
    } catch (error) {
      _showError(error.toString().replaceFirst('Exception: ', ''));
      return false;
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _onCardDismissed(DismissDirection _) {
    final lesson = _lesson!;
    if (_currentIndex == lesson.vocabulary.length - 1) {
      final progress = _progress!;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) {
          return;
        }
        context.pushReplacement(
          '/devvocab-lesson/${lesson.id}/flashcards/summary',
          extra: FlashcardSummaryArgs(lesson: lesson, progress: progress),
        );
      });
      return;
    }
    setState(() => _currentIndex += 1);
  }

  void _showError(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final lesson = _lesson;
    final progress = _progress;
    if (_isLoading || lesson == null || progress == null) {
      return Scaffold(
        backgroundColor: const Color(0xFF0D0B35),
        body: _errorMessage == null
            ? const Center(child: CircularProgressIndicator())
            : _SessionError(message: _errorMessage!, onRetry: _loadSession),
      );
    }

    if (lesson.vocabulary.isEmpty) {
      return Scaffold(
        backgroundColor: const Color(0xFF0D0B35),
        appBar: AppBar(title: Text(lesson.title)),
        body: const Center(
          child: Text(
            'Bài học này chưa có từ vựng.',
            style: TextStyle(color: AppTheme.textSecondary),
          ),
        ),
      );
    }

    final vocabulary = lesson.vocabulary[_currentIndex];
    return Scaffold(
      backgroundColor: const Color(0xFF0D0B35),
      body: SafeArea(
        child: Column(
          children: [
            _StudyHeader(
              index: _currentIndex + 1,
              total: lesson.vocabulary.length,
              onClose: () => context.pop(),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 22, 16, 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _StatusPill(
                    icon: Icons.arrow_back_rounded,
                    label: 'Đang học',
                    value: progress.learningCards,
                    color: const Color(0xFFFF9A36),
                  ),
                  _StatusPill(
                    icon: Icons.arrow_forward_rounded,
                    label: 'Đã biết',
                    value: progress.knownCards,
                    color: const Color(0xFF5CE0B6),
                    reverse: true,
                  ),
                ],
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(28, 20, 28, 12),
                child: Dismissible(
                  key: ValueKey('study-${vocabulary.id}'),
                  direction: DismissDirection.horizontal,
                  dismissThresholds: const {
                    DismissDirection.startToEnd: 0.28,
                    DismissDirection.endToStart: 0.28,
                  },
                  confirmDismiss: _confirmDismiss,
                  onDismissed: _onCardDismissed,
                  background: _SwipeBackground(
                    alignment: Alignment.centerLeft,
                    color: const Color(0xFF5CE0B6),
                    icon: Icons.check_rounded,
                    label: 'Đã biết',
                  ),
                  secondaryBackground: _SwipeBackground(
                    alignment: Alignment.centerRight,
                    color: const Color(0xFFFF9A36),
                    icon: Icons.replay_rounded,
                    label: 'Đang học',
                  ),
                  child: VocabularyFlipCard(
                    vocabulary: vocabulary,
                    showActions: true,
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(36, 8, 36, 28),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '← Vuốt trái: Đang học',
                    style: GoogleFonts.inter(
                      color: const Color(0xFFFFB66D),
                      fontSize: 12,
                    ),
                  ),
                  Text(
                    'Đã biết: Vuốt phải →',
                    style: GoogleFonts.inter(
                      color: const Color(0xFF83ECD0),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StudyHeader extends StatelessWidget {
  final int index;
  final int total;
  final VoidCallback onClose;

  const _StudyHeader({
    required this.index,
    required this.total,
    required this.onClose,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 20),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: Color(0xFF707995), width: 2)),
      ),
      child: Row(
        children: [
          _RoundIconButton(icon: Icons.close_rounded, onPressed: onClose),
          Expanded(
            child: Text(
              '$index / $total',
              textAlign: TextAlign.center,
              style: GoogleFonts.outfit(
                color: Colors.white,
                fontSize: 28,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const _RoundIconButton(icon: Icons.settings_outlined),
        ],
      ),
    );
  }
}

class _RoundIconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback? onPressed;

  const _RoundIconButton({required this.icon, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFF171631),
      shape: const CircleBorder(
        side: BorderSide(color: Color(0xFF30304B), width: 2),
      ),
      child: IconButton(
        icon: Icon(icon, color: Colors.white, size: 31),
        onPressed: onPressed,
        constraints: const BoxConstraints.tightFor(width: 58, height: 58),
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  final IconData icon;
  final String label;
  final int value;
  final Color color;
  final bool reverse;

  const _StatusPill({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
    this.reverse = false,
  });

  @override
  Widget build(BuildContext context) {
    final parts = [
      Icon(icon, color: color, size: 16),
      const SizedBox(width: 5),
      Text(
        '$label $value',
        style: GoogleFonts.outfit(
          color: color,
          fontSize: 13,
          fontWeight: FontWeight.w700,
        ),
      ),
    ];
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.06),
        border: Border.all(color: color, width: 1.5),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: reverse ? parts.reversed.toList() : parts,
      ),
    );
  }
}

class _SwipeBackground extends StatelessWidget {
  final Alignment alignment;
  final Color color;
  final IconData icon;
  final String label;

  const _SwipeBackground({
    required this.alignment,
    required this.color,
    required this.icon,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: alignment,
      padding: const EdgeInsets.symmetric(horizontal: 30),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.18),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: color, size: 30),
          const SizedBox(width: 8),
          Text(
            label,
            style: TextStyle(color: color, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _SessionError extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _SessionError({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.wifi_off_outlined, color: AppTheme.textSecondary),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }
}

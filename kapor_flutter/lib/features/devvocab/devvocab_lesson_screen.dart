import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';

class DevVocabLessonScreen extends StatefulWidget {
  final String topicId;
  final DevVocabTopic? topic;

  const DevVocabLessonScreen({super.key, required this.topicId, this.topic});

  @override
  State<DevVocabLessonScreen> createState() => _DevVocabLessonScreenState();
}

class _DevVocabLessonScreenState extends State<DevVocabLessonScreen> {
  final DevVocabService _devVocabService = DevVocabService();
  List<DevVocabLesson> _lessons = const [];
  Map<String, FlashcardProgress> _progressByLesson = const {};
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadLessons();
  }

  Future<void> _loadLessons() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final lessons = await _devVocabService.getLessons(widget.topicId);
      final progressResults = await Future.wait(
        lessons.map((lesson) => _loadLessonProgress(lesson.id)),
      );
      final progressByLesson = Map<String, FlashcardProgress>.fromEntries(
        progressResults.whereType<FlashcardProgress>().map(
          (progress) => MapEntry(progress.lessonId, progress),
        ),
      );
      if (!mounted) return;
      setState(() {
        _lessons = lessons;
        _progressByLesson = progressByLesson;
        _isLoading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _errorMessage = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<FlashcardProgress?> _loadLessonProgress(String lessonId) async {
    try {
      return await _devVocabService.getFlashcardProgress(lessonId);
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: const Color(0xFF102126),
        surfaceTintColor: Colors.transparent,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(
            height: 1,
            color: AppTheme.primary.withValues(alpha: 0.28),
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(widget.topic?.title ?? 'Danh sách bài học'),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_errorMessage != null) {
      return _StateMessage(
        icon: Icons.wifi_off_outlined,
        message: _errorMessage!,
        actionLabel: 'Thử lại',
        onAction: _loadLessons,
      );
    }
    if (_lessons.isEmpty) {
      return const _StateMessage(
        icon: Icons.menu_book_outlined,
        message: 'Topic này chưa có bài học.',
      );
    }

    final nextLessonId = _nextLessonId();
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      itemCount: _lessons.length + 1,
      itemBuilder: (context, index) {
        if (index == 0) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.topic?.titleVi ?? 'Chọn bài học bạn muốn bắt đầu',
                  style: GoogleFonts.outfit(
                    color: AppTheme.textPrimary,
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${_lessons.length} bài học',
                  style: GoogleFonts.jetBrainsMono(
                    color: AppTheme.textSecondary,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          );
        }

        final lesson = _lessons[index - 1];
        return _LessonListCard(
          lesson: lesson,
          position: index,
          progress: _progressByLesson[lesson.id],
          isNextLesson: lesson.id == nextLessonId,
          showConnector: index < _lessons.length,
          onTap: () =>
              context.push('/devvocab-lesson/${lesson.id}', extra: lesson),
        );
      },
    );
  }

  String? _nextLessonId() {
    for (final lesson in _lessons) {
      if (lesson.vocabulary.isEmpty) continue;

      final progress = _progressByLesson[lesson.id];
      final totalCards = progress?.totalCards ?? 0;
      final reviewedCards =
          (progress?.knownCards ?? 0) + (progress?.learningCards ?? 0);
      final isCompleted =
          totalCards > 0 && (progress?.knownCards ?? 0) >= totalCards;
      final isInProgress = !isCompleted && reviewedCards > 0;

      if (!isInProgress && !isCompleted) return lesson.id;
    }
    return null;
  }
}

class _LessonListCard extends StatefulWidget {
  final DevVocabLesson lesson;
  final int position;
  final FlashcardProgress? progress;
  final bool isNextLesson;
  final bool showConnector;
  final VoidCallback onTap;

  const _LessonListCard({
    required this.lesson,
    required this.position,
    required this.progress,
    required this.isNextLesson,
    required this.showConnector,
    required this.onTap,
  });

  @override
  State<_LessonListCard> createState() => _LessonListCardState();
}

class _LessonListCardState extends State<_LessonListCard> {
  bool _isPressed = false;

  bool get _isCompleted =>
      widget.progress != null &&
      widget.progress!.totalCards > 0 &&
      widget.progress!.knownCards >= widget.progress!.totalCards;

  int get _reviewedCards =>
      (widget.progress?.knownCards ?? 0) +
      (widget.progress?.learningCards ?? 0);

  bool get _isInProgress =>
      !_isCompleted && widget.progress != null && _reviewedCards > 0;

  bool get _isEmpty => widget.lesson.vocabulary.isEmpty;

  Color get _accentColor {
    if (_isCompleted) return const Color(0xFF55D8AD);
    if (_isInProgress || widget.isNextLesson) return AppTheme.primary;
    return AppTheme.textSecondary;
  }

  Color get _cardColor {
    if (_isInProgress) return const Color(0xFF102B31);
    if (_isEmpty) return AppTheme.surface.withValues(alpha: 0.58);
    return AppTheme.surface;
  }

  Color get _borderColor {
    if (_isInProgress) return AppTheme.primary.withValues(alpha: 0.58);
    if (_isCompleted) return Colors.white.withValues(alpha: 0.10);
    if (widget.isNextLesson) return AppTheme.primary.withValues(alpha: 0.30);
    if (_isEmpty) return Colors.white.withValues(alpha: 0.05);
    return Colors.white.withValues(alpha: 0.08);
  }

  double get _progressValue {
    final totalCards = widget.progress?.totalCards ?? 0;
    if (totalCards == 0) return 0;
    return (_reviewedCards / totalCards).clamp(0, 1);
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: _semanticLabel,
      child: Column(
        children: [
          AnimatedScale(
            scale: _isPressed ? 0.98 : 1,
            duration: const Duration(milliseconds: 90),
            curve: Curves.easeOut,
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: widget.onTap,
                onHighlightChanged: (isHighlighted) {
                  setState(() => _isPressed = isHighlighted);
                },
                borderRadius: BorderRadius.circular(18),
                child: Ink(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: _cardColor,
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(color: _borderColor),
                    boxShadow: _isInProgress
                        ? [
                            BoxShadow(
                              color: AppTheme.primary.withValues(alpha: 0.08),
                              blurRadius: 16,
                              spreadRadius: 1,
                            ),
                          ]
                        : null,
                  ),
                  child: _cardContent(),
                ),
              ),
            ),
          ),
          if (widget.showConnector)
            SizedBox(
              height: 14,
              child: Padding(
                padding: const EdgeInsets.only(left: 36),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Container(
                    width: 2,
                    height: 14,
                    color: _accentColor.withValues(alpha: 0.24),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  String get _semanticLabel {
    if (_isCompleted) return 'Bài học ${widget.lesson.title}, đã hoàn thành';
    if (_isInProgress) {
      return 'Tiếp tục bài học ${widget.lesson.title}, $_reviewedCards trên ${widget.progress!.totalCards} thẻ';
    }
    if (_isEmpty) return 'Bài học ${widget.lesson.title}, chưa có nội dung';
    if (widget.isNextLesson) return 'Bắt đầu bài học ${widget.lesson.title}';
    return 'Mở bài học ${widget.lesson.title}';
  }

  Widget _cardContent() {
    return Row(
      children: [
        Container(
          width: 42,
          height: 42,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: _accentColor.withValues(alpha: _isEmpty ? 0.08 : 0.14),
            borderRadius: BorderRadius.circular(13),
          ),
          child: Text(
            widget.position.toString().padLeft(2, '0'),
            style: GoogleFonts.jetBrainsMono(
              color: _accentColor,
              fontWeight: FontWeight.w700,
              fontSize: 12,
            ),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                widget.lesson.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.outfit(
                  color: _isEmpty
                      ? AppTheme.textPrimary.withValues(alpha: 0.62)
                      : AppTheme.textPrimary,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                widget.lesson.titleVi,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.inter(
                  color: AppTheme.textSecondary,
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 9),
              Row(
                children: [
                  Text(
                    '${widget.lesson.vocabulary.length} thuật ngữ',
                    style: GoogleFonts.jetBrainsMono(
                      color: AppTheme.primary,
                      fontSize: 10,
                    ),
                  ),
                  if (_isInProgress) ...[
                    const Spacer(),
                    _StatusLabel(label: 'Tiếp tục', color: AppTheme.primary),
                  ],
                  if (_isCompleted) ...[
                    const Spacer(),
                    _StatusLabel(
                      label: 'Hoàn thành',
                      color: const Color(0xFF55D8AD),
                    ),
                  ],
                  if (widget.isNextLesson) ...[
                    const Spacer(),
                    _StatusLabel(label: 'Bắt đầu', color: AppTheme.primary),
                  ],
                  if (_isEmpty) ...[
                    const Spacer(),
                    _StatusLabel(
                      label: 'Chưa có nội dung',
                      color: AppTheme.textSecondary,
                    ),
                  ],
                ],
              ),
              if (_isInProgress) ...[
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(99),
                  child: LinearProgressIndicator(
                    value: _progressValue,
                    minHeight: 4,
                    backgroundColor: Colors.white.withValues(alpha: 0.08),
                    valueColor: const AlwaysStoppedAnimation(AppTheme.primary),
                  ),
                ),
              ],
              if (_isInProgress) ...[
                const SizedBox(height: 4),
                Text(
                  '$_reviewedCards/${widget.progress!.totalCards} thẻ đã xem',
                  style: GoogleFonts.jetBrainsMono(
                    color: AppTheme.textSecondary,
                    fontSize: 9,
                  ),
                ),
              ],
            ],
          ),
        ),
        if (_isCompleted)
          const Icon(Icons.check_circle_rounded, color: Color(0xFF55D8AD))
        else if (_isEmpty)
          Icon(
            Icons.info_outline_rounded,
            color: AppTheme.textSecondary.withValues(alpha: 0.58),
          )
        else
          const Icon(
            Icons.chevron_right_rounded,
            color: AppTheme.textSecondary,
          ),
      ],
    );
  }
}

class _StatusLabel extends StatelessWidget {
  final String label;
  final Color color;

  const _StatusLabel({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Text(
        label,
        style: GoogleFonts.jetBrainsMono(
          color: color,
          fontSize: 9,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _StateMessage extends StatelessWidget {
  final IconData icon;
  final String message;
  final String? actionLabel;
  final Future<void> Function()? onAction;

  const _StateMessage({
    required this.icon,
    required this.message,
    this.actionLabel,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 34, color: AppTheme.textSecondary),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppTheme.textSecondary),
            ),
            if (onAction != null && actionLabel != null) ...[
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: onAction,
                icon: const Icon(Icons.refresh, size: 16),
                label: Text(actionLabel!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

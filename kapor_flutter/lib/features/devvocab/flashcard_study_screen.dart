import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import '../../core/audio/korean_tts_player.dart';
import 'data/devvocab_service.dart';
import 'flashcard_summary_screen.dart';
import 'widgets/vocabulary_flip_card.dart';
import '../membyte/data/membyte_service.dart';

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
  final MemByteService _memByteService = MemByteService();
  DevVocabLesson? _lesson;
  FlashcardProgress? _progress;
  bool _isLoading = true;
  bool _isSaving = false;
  bool _isUndoing = false;
  bool _isSavingToMemByte = false;
  String? _errorMessage;
  int _currentIndex = 0;
  Set<String> _savedVocabularyIds = {};
  List<LessonVocabularyItem> _sessionVocabulary = [];
  bool _isShuffled = false;
  bool _isAutoSpeakEnabled = false;

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
        _memByteService.getSavedVocabularyIds(widget.lessonId),
      ]);
      if (!mounted) {
        return;
      }
      setState(() {
        _lesson = results[0] as DevVocabLesson;
        _progress = results[1] as FlashcardProgress;
        _savedVocabularyIds = results[2] as Set<String>;
        _sessionVocabulary = List.of(_lesson!.vocabulary);
        _currentIndex = 0;
        _isShuffled = false;
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

  Future<bool> _recordSwipe(_FlashcardDecision decision) async {
    if (_isSaving) {
      return false;
    }
    final lesson = _lesson;
    if (lesson == null || _currentIndex >= _sessionVocabulary.length) {
      return false;
    }
    final vocabulary = _sessionVocabulary[_currentIndex];
    if (vocabulary.id.isEmpty) {
      _showError('Thẻ này chưa có ID. Vui lòng lưu lại Lesson trong Admin.');
      return false;
    }

    final status = decision == _FlashcardDecision.known ? 'KNOWN' : 'LEARNING';
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

  void _onCardSwiped(_FlashcardDecision _) {
    final lesson = _lesson!;
    if (_currentIndex == _sessionVocabulary.length - 1) {
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
    final nextVocabulary = _sessionVocabulary[_currentIndex];
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _playVocabularyAutomatically(nextVocabulary);
    });
  }

  Future<void> _undoPreviousCard() async {
    final lesson = _lesson;
    if (_isUndoing || _isSaving || lesson == null || _currentIndex == 0) {
      return;
    }
    final previousIndex = _currentIndex - 1;
    final vocabulary = _sessionVocabulary[previousIndex];
    if (vocabulary.id.isEmpty) {
      _showError('Thẻ này chưa có ID. Vui lòng lưu lại Lesson trong Admin.');
      return;
    }

    setState(() => _isUndoing = true);
    try {
      final progress = await _devVocabService.resetFlashcardStatus(
        lessonId: lesson.id,
        vocabularyId: vocabulary.id,
      );
      if (!mounted) return;
      setState(() {
        _progress = progress;
        _currentIndex = previousIndex;
      });
    } catch (error) {
      _showError(error.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _isUndoing = false);
    }
  }

  Future<void> _saveCurrentVocabulary() async {
    final lesson = _lesson;
    if (_isSavingToMemByte ||
        lesson == null ||
        _currentIndex >= _sessionVocabulary.length) {
      return;
    }
    final vocabulary = _sessionVocabulary[_currentIndex];
    if (vocabulary.id.isEmpty) {
      _showError('Thẻ này chưa có ID. Vui lòng lưu lại Lesson trong Admin.');
      return;
    }

    setState(() => _isSavingToMemByte = true);
    try {
      await _memByteService.saveVocabulary(
        lessonId: lesson.id,
        vocabularyId: vocabulary.id,
      );
      if (!mounted) return;
      setState(
        () => _savedVocabularyIds = {..._savedVocabularyIds, vocabulary.id},
      );
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã thêm thẻ vào MemByte')));
    } catch (error) {
      _showError(error.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _isSavingToMemByte = false);
    }
  }

  void _setShuffle(bool enabled) {
    final lesson = _lesson;
    if (lesson == null || _sessionVocabulary.isEmpty) return;

    final completed = _sessionVocabulary.take(_currentIndex).toList();
    final current = _sessionVocabulary[_currentIndex];
    final usedKeys = {
      ...completed.map(_vocabularyKey),
      _vocabularyKey(current),
    };
    final remaining = lesson.vocabulary
        .where((vocabulary) => !usedKeys.contains(_vocabularyKey(vocabulary)))
        .toList();

    if (enabled && remaining.length > 1) {
      final originalOrder = remaining.map(_vocabularyKey).toList();
      for (var attempt = 0; attempt < 4; attempt += 1) {
        remaining.shuffle(Random());
        if (!_hasSameVocabularyOrder(
          originalOrder,
          remaining.map(_vocabularyKey).toList(),
        )) {
          break;
        }
      }
      if (_hasSameVocabularyOrder(
        originalOrder,
        remaining.map(_vocabularyKey).toList(),
      )) {
        remaining.setAll(0, remaining.reversed);
      }
    }

    setState(() {
      _sessionVocabulary = [...completed, current, ...remaining];
      _isShuffled = enabled;
    });
  }

  Future<void> _setAutoSpeakEnabled(bool enabled) async {
    setState(() => _isAutoSpeakEnabled = enabled);
    if (!enabled) await KoreanTtsPlayer.instance.stop();
  }

  Future<void> _playVocabularyAutomatically(
    LessonVocabularyItem vocabulary,
  ) async {
    if (!_isAutoSpeakEnabled || vocabulary.korean.trim().isEmpty) return;
    try {
      await KoreanTtsPlayer.instance.playOrStop(vocabulary.korean);
    } on KoreanTtsException {
      // Automatic audio should not interrupt studying with a toast. Users can
      // still retry from the speaker button on the card.
    }
  }

  String _vocabularyKey(LessonVocabularyItem vocabulary) =>
      vocabulary.id.isNotEmpty ? vocabulary.id : vocabulary.korean;

  bool _hasSameVocabularyOrder(List<String> first, List<String> second) {
    if (first.length != second.length) return false;
    for (var index = 0; index < first.length; index += 1) {
      if (first[index] != second[index]) return false;
    }
    return true;
  }

  Future<void> _showOptions() async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => StatefulBuilder(
        builder: (context, setSheetState) => _FlashcardOptionsSheet(
          isShuffled: _isShuffled,
          isAutoSpeakEnabled: _isAutoSpeakEnabled,
          onShuffleChanged: (enabled) {
            _setShuffle(enabled);
            setSheetState(() {});
          },
          onAutoSpeakChanged: (enabled) {
            unawaited(_setAutoSpeakEnabled(enabled));
            setSheetState(() {});
          },
        ),
      ),
    );
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

    if (_sessionVocabulary.isEmpty) {
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

    final vocabulary = _sessionVocabulary[_currentIndex];
    return Scaffold(
      backgroundColor: const Color(0xFF0D0B35),
      body: SafeArea(
        child: Column(
          children: [
            _StudyHeader(
              index: _currentIndex + 1,
              total: _sessionVocabulary.length,
              onClose: () => context.pop(),
              onOptions: _showOptions,
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
                child: _QuizletStyleSwipeCard(
                  key: ValueKey('study-${vocabulary.id}'),
                  isSaving: _isSaving,
                  onSwipe: _recordSwipe,
                  onSwiped: _onCardSwiped,
                  child: VocabularyFlipCard(
                    vocabulary: vocabulary,
                    showActions: true,
                    isSavedToMemByte: _savedVocabularyIds.contains(
                      vocabulary.id,
                    ),
                    isSavingToMemByte: _isSavingToMemByte,
                    onSaveToMemByte: _saveCurrentVocabulary,
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(28, 8, 28, 28),
              child: Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  tooltip: _currentIndex == 0
                      ? 'Chưa có thẻ trước đó'
                      : 'Quay lại thẻ trước',
                  onPressed: _currentIndex == 0 || _isUndoing
                      ? null
                      : _undoPreviousCard,
                  style: IconButton.styleFrom(
                    backgroundColor: const Color(0xFF25254A),
                    foregroundColor: Colors.white,
                    disabledForegroundColor: const Color(0xFF747894),
                    padding: const EdgeInsets.all(12),
                  ),
                  icon: _isUndoing
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.undo_rounded, size: 25),
                ),
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
  final VoidCallback onOptions;

  const _StudyHeader({
    required this.index,
    required this.total,
    required this.onClose,
    required this.onOptions,
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
          _RoundIconButton(icon: Icons.settings_outlined, onPressed: onOptions),
        ],
      ),
    );
  }
}

class _FlashcardOptionsSheet extends StatelessWidget {
  final bool isShuffled;
  final bool isAutoSpeakEnabled;
  final ValueChanged<bool> onShuffleChanged;
  final ValueChanged<bool> onAutoSpeakChanged;

  const _FlashcardOptionsSheet({
    required this.isShuffled,
    required this.isAutoSpeakEnabled,
    required this.onShuffleChanged,
    required this.onAutoSpeakChanged,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        margin: const EdgeInsets.fromLTRB(12, 0, 12, 12),
        padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
        decoration: const BoxDecoration(
          color: Color(0xFF101038),
          borderRadius: BorderRadius.all(Radius.circular(30)),
          border: Border.fromBorderSide(
            BorderSide(color: Color(0xFF34355F), width: 1.5),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: 'Đóng tùy chọn',
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded, color: Colors.white),
                ),
                Expanded(
                  child: Text(
                    'Tùy chọn',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.outfit(
                      color: Colors.white,
                      fontSize: 26,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 48),
              ],
            ),
            const SizedBox(height: 18),
            Container(
              padding: const EdgeInsets.symmetric(vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFF353D62),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Column(
                children: [
                  _OptionSwitchTile(
                    icon: Icons.shuffle_rounded,
                    title: 'Trộn thẻ',
                    subtitle: 'Chỉ thay đổi thứ tự trong lần học này',
                    value: isShuffled,
                    onChanged: onShuffleChanged,
                  ),
                  const Divider(
                    indent: 68,
                    endIndent: 20,
                    color: Color(0xFF4C557C),
                    height: 1,
                  ),
                  _OptionSwitchTile(
                    icon: Icons.volume_up_outlined,
                    title: 'Chuyển văn bản thành lời nói',
                    subtitle: 'Tự phát âm tiếng Hàn ở thẻ tiếp theo',
                    value: isAutoSpeakEnabled,
                    onChanged: onAutoSpeakChanged,
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

class _OptionSwitchTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _OptionSwitchTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      toggled: value,
      child: SwitchListTile.adaptive(
        value: value,
        onChanged: onChanged,
        activeTrackColor: const Color(0xFF5CE0B6),
        activeThumbColor: const Color(0xFF0D0B35),
        contentPadding: const EdgeInsets.fromLTRB(20, 12, 16, 12),
        secondary: Icon(icon, color: const Color(0xFFBEC7E8), size: 26),
        title: Text(
          title,
          style: GoogleFonts.outfit(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 3),
          child: Text(
            subtitle,
            style: GoogleFonts.inter(
              color: const Color(0xFFBBC2DD),
              fontSize: 12,
              height: 1.35,
            ),
          ),
        ),
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

enum _FlashcardDecision { known, learning }

/// A card interaction inspired by Quizlet's swipe flow: the card follows the
/// finger, rotates slightly, and reveals a clear decision surface underneath.
/// The progress update is still confirmed by the API before the card exits.
class _QuizletStyleSwipeCard extends StatefulWidget {
  final Widget child;
  final bool isSaving;
  final Future<bool> Function(_FlashcardDecision decision) onSwipe;
  final ValueChanged<_FlashcardDecision> onSwiped;

  const _QuizletStyleSwipeCard({
    super.key,
    required this.child,
    required this.isSaving,
    required this.onSwipe,
    required this.onSwiped,
  });

  @override
  State<_QuizletStyleSwipeCard> createState() => _QuizletStyleSwipeCardState();
}

class _QuizletStyleSwipeCardState extends State<_QuizletStyleSwipeCard>
    with SingleTickerProviderStateMixin {
  static const _commitFraction = 0.24;
  static const _flingVelocity = 850.0;

  late final AnimationController _motionController;
  double _dragOffset = 0;
  double _cardWidth = 1;
  bool _hasTriggeredHaptic = false;

  @override
  void initState() {
    super.initState();
    _motionController = AnimationController(vsync: this);
  }

  @override
  void dispose() {
    _motionController.dispose();
    super.dispose();
  }

  _FlashcardDecision get _decision =>
      _dragOffset >= 0 ? _FlashcardDecision.known : _FlashcardDecision.learning;

  double get _dragProgress => (_dragOffset.abs() / _cardWidth).clamp(0.0, 1.0);

  bool get _hasReachedCommitPoint => _dragProgress >= _commitFraction;

  void _onDragStart(DragStartDetails _) {
    if (widget.isSaving) return;
    _motionController.stop(canceled: false);
  }

  void _onDragUpdate(DragUpdateDetails details) {
    if (widget.isSaving) return;
    final nextOffset = (_dragOffset + details.delta.dx).clamp(
      -_cardWidth * 0.92,
      _cardWidth * 0.92,
    );
    final wasPastCommitPoint = _hasReachedCommitPoint;
    setState(() => _dragOffset = nextOffset);

    if (!wasPastCommitPoint && _hasReachedCommitPoint && !_hasTriggeredHaptic) {
      HapticFeedback.selectionClick();
      _hasTriggeredHaptic = true;
    } else if (!_hasReachedCommitPoint) {
      _hasTriggeredHaptic = false;
    }
  }

  Future<void> _onDragEnd(DragEndDetails details) async {
    if (widget.isSaving) return;
    final shouldCommit =
        _hasReachedCommitPoint ||
        (details.velocity.pixelsPerSecond.dx.abs() > _flingVelocity &&
            _dragProgress > 0.08);
    setState(() {
      _hasTriggeredHaptic = false;
    });

    if (!shouldCommit) {
      await _animateTo(0, const Duration(milliseconds: 260));
      return;
    }

    final decision = _decision;
    final accepted = await widget.onSwipe(decision);
    if (!mounted) return;
    if (!accepted) {
      await _animateTo(0, const Duration(milliseconds: 280));
      return;
    }

    final destination =
        (decision == _FlashcardDecision.known ? 1 : -1) * _cardWidth * 1.35;
    await _animateTo(destination, const Duration(milliseconds: 330));
    if (mounted) widget.onSwiped(decision);
  }

  void _onDragCancel() {
    if (widget.isSaving) return;
    setState(() => _hasTriggeredHaptic = false);
    _animateTo(0, const Duration(milliseconds: 220));
  }

  Future<void> _animateTo(double target, Duration duration) async {
    if (_dragOffset == target) return;
    if (MediaQuery.maybeOf(context)?.disableAnimations ?? false) {
      setState(() => _dragOffset = target);
      return;
    }
    final animation = Tween<double>(begin: _dragOffset, end: target).animate(
      CurvedAnimation(parent: _motionController, curve: Curves.easeOutCubic),
    );
    void listener() {
      if (mounted) setState(() => _dragOffset = animation.value);
    }

    _motionController
      ..stop()
      ..reset()
      ..duration = duration
      ..addListener(listener);
    try {
      await _motionController.forward();
    } finally {
      _motionController.removeListener(listener);
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        _cardWidth = constraints.maxWidth;
        final progress = _dragProgress;
        final decision = _decision;
        final isKnown = decision == _FlashcardDecision.known;
        final accent = isKnown
            ? const Color(0xFF5CE0B6)
            : const Color(0xFFFF9A36);
        final reduceMotion =
            MediaQuery.maybeOf(context)?.disableAnimations ?? false;
        final rotation = reduceMotion ? 0.0 : _dragOffset / _cardWidth * 0.115;

        return Semantics(
          label:
              'Thẻ ghi nhớ. Kéo sang phải để đánh dấu đã biết, kéo sang trái để đánh dấu đang học.',
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            onHorizontalDragStart: _onDragStart,
            onHorizontalDragUpdate: _onDragUpdate,
            onHorizontalDragEnd: _onDragEnd,
            onHorizontalDragCancel: _onDragCancel,
            child: Stack(
              fit: StackFit.expand,
              children: [
                _SwipeDecisionBackdrop(decision: decision, progress: progress),
                Transform.translate(
                  offset: Offset(_dragOffset, 0),
                  child: Transform.rotate(
                    angle: rotation,
                    alignment: _dragOffset >= 0
                        ? Alignment.bottomLeft
                        : Alignment.bottomRight,
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        widget.child,
                        IgnorePointer(
                          child: Opacity(
                            opacity: (progress * 1.45).clamp(0.0, 1.0),
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(28),
                                border: Border.all(color: accent, width: 4),
                                color: accent.withValues(alpha: 0.055),
                              ),
                              child: Align(
                                alignment: isKnown
                                    ? Alignment.centerLeft
                                    : Alignment.centerRight,
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 30,
                                  ),
                                  child: _CardDecisionLabel(
                                    decision: decision,
                                    scale: 0.9 + (progress * 0.25),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                if (widget.isSaving)
                  const IgnorePointer(
                    child: Center(
                      child: CircularProgressIndicator(color: Colors.white),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _SwipeDecisionBackdrop extends StatelessWidget {
  final _FlashcardDecision decision;
  final double progress;

  const _SwipeDecisionBackdrop({
    required this.decision,
    required this.progress,
  });

  @override
  Widget build(BuildContext context) {
    final isKnown = decision == _FlashcardDecision.known;
    final color = isKnown ? const Color(0xFF5CE0B6) : const Color(0xFFFF9A36);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.10 + (progress * 0.20)),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Align(
        alignment: isKnown ? Alignment.centerLeft : Alignment.centerRight,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 22),
          child: Opacity(
            opacity: (progress * 1.8).clamp(0.0, 1.0),
            child: _CardDecisionLabel(decision: decision, scale: 1),
          ),
        ),
      ),
    );
  }
}

class _CardDecisionLabel extends StatelessWidget {
  final _FlashcardDecision decision;
  final double scale;

  const _CardDecisionLabel({required this.decision, required this.scale});

  @override
  Widget build(BuildContext context) {
    final isKnown = decision == _FlashcardDecision.known;
    final color = isKnown ? const Color(0xFF5CE0B6) : const Color(0xFFFF9A36);
    return Transform.scale(
      scale: scale,
      alignment: isKnown ? Alignment.centerLeft : Alignment.centerRight,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: isKnown
            ? CrossAxisAlignment.start
            : CrossAxisAlignment.end,
        children: [
          Icon(
            isKnown ? Icons.check_circle_rounded : Icons.replay_rounded,
            color: color,
            size: 42,
          ),
          const SizedBox(height: 8),
          Text(
            isKnown ? 'Đã biết' : 'Đang học',
            style: GoogleFonts.outfit(
              color: color,
              fontSize: 23,
              fontWeight: FontWeight.w800,
            ),
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

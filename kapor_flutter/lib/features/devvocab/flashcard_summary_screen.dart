import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import 'data/devvocab_service.dart';

class FlashcardSummaryArgs {
  final DevVocabLesson lesson;
  final FlashcardProgress progress;

  const FlashcardSummaryArgs({required this.lesson, required this.progress});
}

class FlashcardSummaryScreen extends StatefulWidget {
  final FlashcardSummaryArgs args;

  const FlashcardSummaryScreen({super.key, required this.args});

  @override
  State<FlashcardSummaryScreen> createState() => _FlashcardSummaryScreenState();
}

class _FlashcardSummaryScreenState extends State<FlashcardSummaryScreen> {
  final DevVocabService _devVocabService = DevVocabService();
  bool _isResetting = false;

  Future<void> _reset() async {
    setState(() => _isResetting = true);
    try {
      await _devVocabService.resetFlashcardProgress(widget.args.lesson.id);
      if (!mounted) return;
      context.pushReplacement(
        '/devvocab-lesson/${widget.args.lesson.id}/flashcards',
        extra: widget.args.lesson,
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    } finally {
      if (mounted) setState(() => _isResetting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final progress = widget.args.progress;
    final total = progress.totalCards == 0
        ? widget.args.lesson.vocabulary.length
        : progress.totalCards;
    final knownPercent = total == 0 ? 0.0 : progress.knownCards / total;

    return Scaffold(
      backgroundColor: const Color(0xFF0D0B35),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(28, 18, 28, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _CloseButton(onPressed: () => context.pop()),
                  Expanded(
                    child: Text(
                      '$total / $total',
                      textAlign: TextAlign.center,
                      style: GoogleFonts.outfit(
                        color: Colors.white,
                        fontSize: 27,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  const SizedBox(width: 58),
                ],
              ),
              const Spacer(flex: 2),
              Text(
                'Bạn thật cừ! Bạn đã hoàn thành thẻ ghi nhớ.',
                style: GoogleFonts.outfit(
                  color: Colors.white,
                  fontSize: 30,
                  fontWeight: FontWeight.w800,
                  height: 1.15,
                ),
              ),
              const SizedBox(height: 42),
              Text(
                'Tiến độ của bạn',
                style: GoogleFonts.outfit(
                  color: const Color(0xFFE2E4F2),
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 26),
              Row(
                children: [
                  SizedBox(
                    width: 170,
                    height: 170,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        SizedBox(
                          width: 170,
                          height: 170,
                          child: CircularProgressIndicator(
                            value: knownPercent,
                            strokeWidth: 27,
                            backgroundColor: const Color(0xFFFF9A36),
                            valueColor: const AlwaysStoppedAnimation(
                              Color(0xFF5CE0B6),
                            ),
                          ),
                        ),
                        Text(
                          '${(knownPercent * 100).round()}%',
                          style: GoogleFonts.outfit(
                            color: const Color(0xFFE2E4F2),
                            fontSize: 38,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 20),
                  Expanded(
                    child: Column(
                      children: [
                        _SummaryCount(
                          label: 'Đã biết',
                          value: progress.knownCards,
                          color: const Color(0xFF5CE0B6),
                        ),
                        const SizedBox(height: 12),
                        _SummaryCount(
                          label: 'Đang học',
                          value: progress.learningCards,
                          color: const Color(0xFFFF9A36),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const Spacer(flex: 3),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => context.pushReplacement(
                    '/devvocab-lesson/${widget.args.lesson.id}/flashcards',
                    extra: widget.args.lesson,
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF7183F8),
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(56),
                  ),
                  child: const Text('Ôn luyện lại các thẻ'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _isResetting ? null : _reset,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF626D91),
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(56),
                  ),
                  child: Text(
                    _isResetting ? 'Đang đặt lại...' : 'Đặt lại Thẻ ghi nhớ',
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Center(
                child: TextButton(
                  onPressed: () => context.pushReplacement(
                    '/devvocab-lesson/${widget.args.lesson.id}',
                    extra: widget.args.lesson,
                  ),
                  child: const Text('Quay lại bài học'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CloseButton extends StatelessWidget {
  final VoidCallback onPressed;

  const _CloseButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFF171631),
      shape: const CircleBorder(
        side: BorderSide(color: Color(0xFF30304B), width: 2),
      ),
      child: IconButton(
        onPressed: onPressed,
        constraints: const BoxConstraints.tightFor(width: 58, height: 58),
        icon: const Icon(Icons.close_rounded, color: Colors.white, size: 31),
      ),
    );
  }
}

class _SummaryCount extends StatelessWidget {
  final String label;
  final int value;
  final Color color;

  const _SummaryCount({
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: GoogleFonts.outfit(
              color: const Color(0xFF26314C),
              fontSize: 17,
              fontWeight: FontWeight.w800,
            ),
          ),
          Text(
            '$value',
            style: GoogleFonts.outfit(
              color: const Color(0xFF26314C),
              fontSize: 20,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

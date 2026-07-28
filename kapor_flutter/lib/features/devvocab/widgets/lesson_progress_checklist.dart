import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../core/theme/app_theme.dart';
import '../data/devvocab_service.dart';

class LessonProgressChecklist extends StatelessWidget {
  final LessonActivityProgress? progress;

  const LessonProgressChecklist({super.key, required this.progress});

  @override
  Widget build(BuildContext context) {
    final studyDone = progress?.studyCompleted == true;
    final quizDone = progress?.quizPassed == true;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF16263B),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.primary.withValues(alpha: .22)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'TIẾN ĐỘ HOÀN THÀNH',
            style: GoogleFonts.jetBrainsMono(
              color: AppTheme.primary,
              fontSize: 10,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 9),
          _Item(label: 'Hoàn thành phần Học', done: studyDone),
          const SizedBox(height: 6),
          _Item(label: 'Đạt Kiểm tra từ 80%', done: quizDone),
        ],
      ),
    );
  }
}

class _Item extends StatelessWidget {
  final String label;
  final bool done;
  const _Item({required this.label, required this.done});
  @override
  Widget build(BuildContext context) => Row(
    children: [
      Icon(
        done
            ? Icons.check_circle_rounded
            : Icons.radio_button_unchecked_rounded,
        size: 17,
        color: done ? const Color(0xFF55D8AD) : AppTheme.textSecondary,
      ),
      const SizedBox(width: 8),
      Text(
        label,
        style: GoogleFonts.inter(
          color: done ? const Color(0xFFE1F8EF) : AppTheme.textSecondary,
          fontSize: 12,
        ),
      ),
    ],
  );
}

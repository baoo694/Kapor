import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';

class TechTalkResultScreen extends StatelessWidget {
  const TechTalkResultScreen({super.key, this.session});
  final RoleplaySession? session;
  @override
  Widget build(BuildContext context) {
    final evaluation = session?.finalEvaluation;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Kết quả TechTalk'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.go('/dashboard'),
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: evaluation == null
              ? const Text('Chưa có kết quả phiên roleplay.')
              : Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '${evaluation.overallScore}',
                      style: GoogleFonts.outfit(
                        fontSize: 64,
                        fontWeight: FontWeight.w800,
                        color: AppTheme.primary,
                      ),
                    ),
                    Text(
                      'ĐIỂM TỔNG',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 22),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        _score('Grammar', evaluation.grammar),
                        _score('Vocabulary', evaluation.vocabulary),
                        _score('Politeness', evaluation.politeness),
                        _score('Task', evaluation.taskCompletion),
                      ],
                    ),
                    const SizedBox(height: 22),
                    Text(
                      evaluation.feedbackVi,
                      textAlign: TextAlign.center,
                      style: GoogleFonts.inter(
                        color: AppTheme.textSecondary,
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: 24),
                    ElevatedButton(
                      onPressed: () => context.go('/techtalk-select'),
                      child: const Text('Luyện tình huống khác'),
                    ),
                  ],
                ),
        ),
      ),
    );
  }

  Widget _score(String label, int score) => Column(
    children: [
      Text(
        '$score',
        style: GoogleFonts.outfit(fontSize: 21, fontWeight: FontWeight.w700),
      ),
      Text(
        label,
        style: GoogleFonts.jetBrainsMono(
          fontSize: 9,
          color: AppTheme.textSecondary,
        ),
      ),
    ],
  );
}

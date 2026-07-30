import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/providers/settings_provider.dart';
import 'data/techtalk_service.dart';
import 'techtalk_strings.dart';

class TechTalkResultScreen extends StatelessWidget {
  const TechTalkResultScreen({super.key, this.session});
  final RoleplaySession? session;
  @override
  Widget build(BuildContext context) {
    final evaluation = session?.finalEvaluation;
    final strings = TechTalkStrings(
      context.watch<SettingsProvider>().localeCode,
    );
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: Text(strings.result),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.go('/dashboard'),
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: evaluation == null
              ? Text(strings.noResult)
              : SingleChildScrollView(
                  child: Column(
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
                        strings.overall,
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 11,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 22),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _score(strings.grammar, evaluation.grammar),
                          _score(strings.vocabulary, evaluation.vocabulary),
                          _score(strings.politeness, evaluation.politeness),
                          _score(strings.task, evaluation.taskCompletion),
                        ],
                      ),
                      const SizedBox(height: 22),
                      Text(
                        strings.en && evaluation.feedback.isNotEmpty
                            ? evaluation.feedback
                            : evaluation.feedbackVi,
                        textAlign: TextAlign.center,
                        style: GoogleFonts.inter(
                          color: AppTheme.textSecondary,
                          height: 1.5,
                        ),
                      ),
                      if (evaluation.objectives.isNotEmpty) ...[
                        const SizedBox(height: 20),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text(
                            strings.objectives.toUpperCase(),
                            style: GoogleFonts.jetBrainsMono(
                              color: AppTheme.textSecondary,
                              fontSize: 10,
                            ),
                          ),
                        ),
                        ...evaluation.objectives.map(
                          (objective) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(
                              objective.completed
                                  ? Icons.check_circle
                                  : Icons.radio_button_unchecked,
                              color: objective.completed
                                  ? AppTheme.primary
                                  : AppTheme.textSecondary,
                            ),
                            title: Text(objective.objective),
                            subtitle: objective.evidence.isEmpty
                                ? null
                                : Text(objective.evidence),
                          ),
                        ),
                      ],
                      if (evaluation.improvementAreas.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text(
                            strings.improvements.toUpperCase(),
                            style: GoogleFonts.jetBrainsMono(
                              color: AppTheme.textSecondary,
                              fontSize: 10,
                            ),
                          ),
                        ),
                        ...evaluation.improvementAreas.map(
                          (item) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            dense: true,
                            leading: const Icon(
                              Icons.arrow_right,
                              color: AppTheme.secondary,
                            ),
                            title: Text(item),
                          ),
                        ),
                      ],
                      const SizedBox(height: 24),
                      ElevatedButton(
                        onPressed: () => context.go('/techtalk-select'),
                        child: Text(strings.anotherScenario),
                      ),
                    ],
                  ),
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

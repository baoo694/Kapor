import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../core/providers/settings_provider.dart';
import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';
import 'techtalk_strings.dart';

/// Displays a saved TechTalk conversation without exposing any send controls.
class TechTalkTranscriptScreen extends StatelessWidget {
  const TechTalkTranscriptScreen({super.key, required this.session});

  final RoleplaySession session;

  @override
  Widget build(BuildContext context) {
    final strings = TechTalkStrings(
      context.watch<SettingsProvider>().localeCode,
    );
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: Text(strings.transcript),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Center(
              child: Chip(
                avatar: const Icon(Icons.visibility_outlined, size: 16),
                label: Text(strings.readOnly),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ),
        ],
      ),
      body: session.messages.isEmpty
          ? Center(child: Text(strings.emptyTranscript))
          : ListView(
              padding: const EdgeInsets.all(14),
              children: [
                Text(
                  strings.transcriptDescription,
                  style: GoogleFonts.inter(
                    color: AppTheme.textSecondary,
                    fontSize: 12,
                  ),
                ),
                const SizedBox(height: 16),
                ...session.messages.map(
                  (message) =>
                      _MessageBubble(message: message, strings: strings),
                ),
              ],
            ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message, required this.strings});

  final RoleplayMessage message;
  final TechTalkStrings strings;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == 'user';
    final evaluation = message.evaluation;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 340),
        margin: const EdgeInsets.only(bottom: 12),
        child: Column(
          crossAxisAlignment: isUser
              ? CrossAxisAlignment.end
              : CrossAxisAlignment.start,
          children: [
            if (isUser && message.source == 'voice')
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.graphic_eq, size: 14),
                    const SizedBox(width: 4),
                    Text(
                      strings.voiceMessage,
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 9,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: isUser ? AppTheme.primary : AppTheme.surface,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text(
                message.content,
                style: GoogleFonts.inter(
                  color: isUser ? Colors.black : AppTheme.textPrimary,
                  height: 1.45,
                ),
              ),
            ),
            if (evaluation != null)
              _Evaluation(evaluation: evaluation, strings: strings),
          ],
        ),
      ),
    );
  }
}

class _Evaluation extends StatelessWidget {
  const _Evaluation({required this.evaluation, required this.strings});

  final MessageEvaluation evaluation;
  final TechTalkStrings strings;

  @override
  Widget build(BuildContext context) {
    if (evaluation.status == 'unavailable') {
      return Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          evaluation.feedbackVi.isEmpty
              ? strings.evaluationUnavailable
              : evaluation.feedbackVi,
          style: GoogleFonts.jetBrainsMono(
            fontSize: 9,
            color: AppTheme.textSecondary,
          ),
        ),
      );
    }
    return Container(
      margin: const EdgeInsets.only(top: 5),
      constraints: const BoxConstraints(maxWidth: 320),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            'G ${evaluation.grammar} · V ${evaluation.vocabulary} · P ${evaluation.politeness}',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 10,
              color: AppTheme.textSecondary,
            ),
          ),
          if (evaluation.feedbackVi.isNotEmpty)
            Text(
              evaluation.feedbackVi,
              textAlign: TextAlign.right,
              style: GoogleFonts.inter(
                fontSize: 10,
                color: AppTheme.textSecondary,
              ),
            ),
          if (evaluation.corrections.isNotEmpty)
            ExpansionTile(
              tilePadding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              title: Text(
                '${strings.corrections} (${evaluation.corrections.length})',
                style: GoogleFonts.inter(fontSize: 11),
              ),
              children: evaluation.corrections
                  .map(
                    (correction) => ListTile(
                      dense: true,
                      title: Text(
                        '${correction.original} → ${correction.suggestion}',
                        style: GoogleFonts.inter(fontSize: 12),
                      ),
                      subtitle: Text(correction.noteVi),
                    ),
                  )
                  .toList(),
            ),
        ],
      ),
    );
  }
}

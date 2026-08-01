import 'package:flutter/material.dart';

class WrongSentenceAlert extends StatelessWidget {
  const WrongSentenceAlert({
    super.key,
    required this.expectedText,
    required this.transcriptText,
    required this.message,
    required this.onRetry,
  });

  final String expectedText;
  final String transcriptText;
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final error = colors.error;
    final transcript = transcriptText.trim().isEmpty
        ? 'WhisperX không nhận dạng được nội dung rõ ràng.'
        : transcriptText.trim();
    final description = message.trim().isEmpty
        ? 'Nội dung bản ghi không khớp câu mẫu. Hãy nghe lại và đọc lại từ đầu.'
        : message.trim();

    return Semantics(
      container: true,
      liveRegion: true,
      child: Card(
        margin: const EdgeInsets.only(top: 12),
        color: error.withValues(alpha: .08),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: error.withValues(alpha: .65)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.report_gmailerrorred_outlined, color: error),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Semantics(
                      header: true,
                      child: Text(
                        'Bạn đã đọc khác câu mẫu',
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(
                              color: error,
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                description,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: colors.onSurfaceVariant,
                  height: 1.45,
                ),
              ),
              const SizedBox(height: 14),
              _EvidenceBlock(label: 'CÂU CẦN ĐỌC', text: expectedText),
              const SizedBox(height: 10),
              _EvidenceBlock(
                label: 'WHISPERX NGHE ĐƯỢC',
                text: transcript,
                emphasized: true,
              ),
              const SizedBox(height: 12),
              Text(
                'Lần đọc này không được tính điểm phát âm.',
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: colors.onSurfaceVariant),
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.mic_none_rounded),
                  label: const Text('Ghi âm và đọc lại'),
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size.fromHeight(48),
                    backgroundColor: error,
                    foregroundColor: colors.onError,
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

class _EvidenceBlock extends StatelessWidget {
  const _EvidenceBlock({
    required this.label,
    required this.text,
    this.emphasized = false,
  });

  final String label;
  final String text;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.onSurface.withValues(alpha: .05),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: colors.onSurface.withValues(alpha: .10)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: colors.onSurfaceVariant,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.15,
            ),
          ),
          const SizedBox(height: 6),
          SelectableText(
            text.trim().isEmpty ? '—' : text.trim(),
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: colors.onSurface,
              fontWeight: emphasized ? FontWeight.w700 : FontWeight.w500,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

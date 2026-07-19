import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../core/theme/app_theme.dart';
import 'data/membyte_service.dart';

class MemByteScreen extends StatefulWidget {
  const MemByteScreen({super.key});

  @override
  State<MemByteScreen> createState() => _MemByteScreenState();
}

class _MemByteScreenState extends State<MemByteScreen> {
  final MemByteService _memByteService = MemByteService();
  List<MemByteDeck> _decks = const [];
  MemByteReviewSummary _summary = const MemByteReviewSummary(
    totalDueCards: 0,
    decksWithDueCards: 0,
  );
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadMemByte();
  }

  Future<void> _loadMemByte() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final results = await Future.wait([
        _memByteService.getDecks(),
        _memByteService.getReviewSummary(),
      ]);
      if (!mounted) return;
      setState(() {
        _decks = results[0] as List<MemByteDeck>;
        _summary = results[1] as MemByteReviewSummary;
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          children: [
            const Text(
              'MemByte',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
            ),
            Text(
              'Hệ thống ôn tập ngắt quãng',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                fontSize: 10,
                color: AppTheme.textSecondary,
              ),
            ),
          ],
        ),
      ),
      body: RefreshIndicator(
        onRefresh: _loadMemByte,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null
            ? _ErrorState(message: _errorMessage!, onRetry: _loadMemByte)
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildReviewHero(context),
                  const SizedBox(height: 24),
                  Text(
                    'BỘ THẺ CỦA BẠN',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: AppTheme.textSecondary,
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (_decks.isEmpty)
                    const _EmptyDeckState()
                  else
                    ..._decks.map((deck) => _buildDeckCard(context, deck)),
                ],
              ),
      ),
    );
  }

  Widget _buildReviewHero(BuildContext context) {
    final hasDueCards = _summary.totalDueCards > 0;
    final deckLabel = _summary.decksWithDueCards == 1 ? 'bộ' : 'bộ';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
        boxShadow: [
          BoxShadow(
            color: AppTheme.secondary.withValues(alpha: 0.1),
            blurRadius: 20,
            spreadRadius: -5,
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.secondary.withValues(alpha: 0.15),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              LucideIcons.brain,
              color: AppTheme.secondary,
              size: 36,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            '${_summary.totalDueCards} thẻ cần ôn tập',
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: AppTheme.textPrimary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            hasDueCards
                ? 'Hôm nay bạn có thẻ từ ${_summary.decksWithDueCards} $deckLabel cần ôn lại.'
                : 'Chưa có thẻ đến hạn. Hãy lưu thẻ từ DevVocab để bắt đầu.',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: hasDueCards
                  ? () => context.push('/membyte-review/all')
                  : null,
              icon: const Icon(LucideIcons.play, color: Colors.black, size: 18),
              label: const Text(
                'Bắt đầu phiên ôn tập ngay',
                style: TextStyle(
                  color: Colors.black,
                  fontWeight: FontWeight.bold,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.secondary,
                disabledBackgroundColor: AppTheme.textSecondary.withValues(
                  alpha: 0.3,
                ),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDeckCard(BuildContext context, MemByteDeck deck) {
    final decoration = _deckDecoration(deck.domain);
    return GestureDetector(
      onTap: () => context.push('/membyte-review/${deck.id}'),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: decoration.color.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: decoration.color.withValues(alpha: 0.3),
                ),
              ),
              alignment: Alignment.center,
              child: Text(
                decoration.emoji,
                style: const TextStyle(fontSize: 20),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    deck.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    deck.titleVi,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontSize: 11,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _buildStatBadge(
                        context,
                        '${deck.totalCards} thẻ',
                        Colors.grey,
                      ),
                      if (deck.dueCards > 0) ...[
                        const SizedBox(width: 6),
                        _buildStatBadge(
                          context,
                          '${deck.dueCards} ôn tập',
                          AppTheme.secondary,
                        ),
                      ],
                      if (deck.newCards > 0) ...[
                        const SizedBox(width: 6),
                        _buildStatBadge(
                          context,
                          '+${deck.newCards} mới',
                          AppTheme.primary,
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            const Icon(
              LucideIcons.chevronRight,
              color: AppTheme.textSecondary,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatBadge(BuildContext context, String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Text(
        text,
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
          fontSize: 9,
          color: color,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  _DeckDecoration _deckDecoration(String domain) {
    return switch (domain.toLowerCase()) {
      'backend' => const _DeckDecoration('⚡', Colors.purpleAccent),
      'devops' => const _DeckDecoration('🔧', Colors.orangeAccent),
      'agile' => const _DeckDecoration('🎯', Colors.greenAccent),
      _ => const _DeckDecoration('🚀', AppTheme.primary),
    };
  }
}

class _DeckDecoration {
  final String emoji;
  final Color color;

  const _DeckDecoration(this.emoji, this.color);
}

class _EmptyDeckState extends StatelessWidget {
  const _EmptyDeckState();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Text(
        'Chưa có bộ thẻ nào. Khi đang học flashcard trong một Lesson, hãy nhấn dấu sao để lưu thẻ vào MemByte.',
        textAlign: TextAlign.center,
        style: TextStyle(color: AppTheme.textSecondary, height: 1.5),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        const SizedBox(height: 180),
        Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ),
        ),
      ],
    );
  }
}

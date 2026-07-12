import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_theme.dart';

class FlashDeck {
  final String id;
  final String name;
  final String nameVi;
  final String domain;
  final int cards;
  final int due;
  final int newCards;
  final String emoji;
  final Color color;

  FlashDeck({
    required this.id,
    required this.name,
    required this.nameVi,
    required this.domain,
    required this.cards,
    required this.due,
    required this.newCards,
    required this.emoji,
    required this.color,
  });
}

final List<FlashDeck> mockFlashDecks = [
  FlashDeck(id: "d1", name: "Frontend Deployment 용어", nameVi: "Từ vựng Deployment Frontend", domain: "frontend", cards: 25, due: 5, newCards: 3, emoji: "🚀", color: AppTheme.primary),
  FlashDeck(id: "d2", name: "API & 서버 용어", nameVi: "Từ vựng API & Server", domain: "backend", cards: 18, due: 2, newCards: 1, emoji: "⚡", color: Colors.purpleAccent),
  FlashDeck(id: "d3", name: "DevOps 핵심 용어", nameVi: "Thuật ngữ cốt lõi DevOps", domain: "devops", cards: 30, due: 8, newCards: 5, emoji: "🔧", color: Colors.orangeAccent),
];

class MemByteScreen extends StatelessWidget {
  const MemByteScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          children: [
            const Text('MemByte', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20)),
            Text(
              'Hệ thống ôn tập ngắt quãng',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(fontSize: 10, color: AppTheme.textSecondary),
            ),
          ],
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
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
            ...mockFlashDecks.map((deck) => _buildDeckCard(context, deck)),
          ],
        ),
      ),
    );
  }

  Widget _buildReviewHero(BuildContext context) {
    int totalDue = mockFlashDecks.fold(0, (sum, deck) => sum + deck.due);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
        boxShadow: [
          BoxShadow(
            color: AppTheme.secondary.withOpacity(0.1),
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
              color: AppTheme.secondary.withOpacity(0.15),
              shape: BoxShape.circle,
            ),
            child: const Icon(LucideIcons.brain, color: AppTheme.secondary, size: 36),
          ),
          const SizedBox(height: 16),
          Text(
            '$totalDue thẻ cần ôn tập',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
          ),
          const SizedBox(height: 4),
          const Text(
            'Hôm nay bạn có thẻ từ 3 bộ cần ôn lại.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () {
                context.push('/membyte-review/all');
              },
              icon: const Icon(LucideIcons.play, color: Colors.black, size: 18),
              label: const Text('Bắt đầu phiên ôn tập ngay', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.secondary,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDeckCard(BuildContext context, FlashDeck deck) {
    return GestureDetector(
      onTap: () {
        context.push('/membyte-review/${deck.id}');
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: deck.color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: deck.color.withOpacity(0.3)),
            ),
            alignment: Alignment.center,
            child: Text(deck.emoji, style: const TextStyle(fontSize: 20)),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  deck.name,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: AppTheme.textPrimary),
                ),
                const SizedBox(height: 4),
                Text(
                  deck.nameVi,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    _buildStatBadge(context, '${deck.cards} thẻ', Colors.grey),
                    if (deck.due > 0) ...[
                      const SizedBox(width: 6),
                      _buildStatBadge(context, '${deck.due} ôn tập', AppTheme.secondary),
                    ],
                    if (deck.newCards > 0) ...[
                      const SizedBox(width: 6),
                      _buildStatBadge(context, '+${deck.newCards} mới', AppTheme.primary),
                    ],
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          const Icon(LucideIcons.chevronRight, color: AppTheme.textSecondary, size: 20),
        ],
      ),
    ),
    );
  }

  Widget _buildStatBadge(BuildContext context, String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withOpacity(0.3)),
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
}

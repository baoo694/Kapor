import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class PronunciationListScreen extends StatelessWidget {
  const PronunciationListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final List<Map<String, dynamic>> exercises = [
      {
        "id": "e1",
        "phrase": "서버 배포가 완료되었습니다",
        "phraseVi": "Việc triển khai server đã hoàn tất",
        "difficulty": "easy",
        "domain": "devops",
        "attempts": 3,
        "bestScore": 82
      },
      {
        "id": "e2",
        "phrase": "비동기 처리를 구현했습니다",
        "phraseVi": "Tôi đã triển khai xử lý bất đồng bộ",
        "difficulty": "medium",
        "domain": "frontend",
        "attempts": 0,
        "bestScore": 0
      }
    ];

    Color getDiffColor(String diff) {
      if (diff == 'easy') return const Color(0xFF34D399);
      if (diff == 'medium') return AppTheme.secondary;
      return Colors.red[300]!;
    }

    Color getDomColor(String dom) {
      if (dom == 'frontend') return AppTheme.primary;
      if (dom == 'devops') return const Color(0xFFFB923C);
      return const Color(0xFFA78BFA);
    }

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Luyện phát âm'),
      ),
      body: SafeArea(
        child: ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: exercises.length,
          itemBuilder: (context, index) {
            final ex = exercises[index];
            return Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: InkWell(
                onTap: () => context.push('/pronunciation'),
                borderRadius: BorderRadius.circular(16),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  ex["phrase"],
                                  style: GoogleFonts.outfit(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 15,
                                    color: AppTheme.textPrimary,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  ex["phraseVi"],
                                  style: GoogleFonts.inter(
                                    fontSize: 10,
                                    color: AppTheme.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              _buildBadge(ex["difficulty"], getDiffColor(ex["difficulty"])),
                              const SizedBox(height: 4),
                              _buildBadge(ex["domain"], getDomColor(ex["domain"])),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Text(
                            '${ex["attempts"]} lần',
                            style: GoogleFonts.jetBrainsMono(
                              fontSize: 10,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                          const SizedBox(width: 10),
                          if (ex["bestScore"] > 0) ...[
                            Expanded(
                              child: Container(
                                height: 3,
                                decoration: BoxDecoration(
                                  color: AppTheme.textSecondary.withOpacity(0.2),
                                  borderRadius: BorderRadius.circular(2),
                                ),
                                child: FractionallySizedBox(
                                  alignment: Alignment.centerLeft,
                                  widthFactor: ex["bestScore"] / 100.0,
                                  child: Container(
                                    decoration: BoxDecoration(
                                      color: AppTheme.primary,
                                      borderRadius: BorderRadius.circular(2),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Text(
                              'Tốt nhất: ${ex["bestScore"]}',
                              style: GoogleFonts.jetBrainsMono(
                                fontSize: 10,
                                color: AppTheme.primary,
                              ),
                            ),
                          ] else ...[
                            Expanded(
                              child: Text(
                                'Chưa thử',
                                style: GoogleFonts.jetBrainsMono(
                                  fontSize: 10,
                                  color: AppTheme.textSecondary.withOpacity(0.7),
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildBadge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        text,
        style: GoogleFonts.jetBrainsMono(
          fontSize: 9,
          color: color,
        ),
      ),
    );
  }
}

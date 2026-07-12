import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class TechTalkSelectScreen extends StatelessWidget {
  const TechTalkSelectScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final List<Map<String, dynamic>> scenarios = [
      {
        "id": "s1",
        "avatar": "👨🏻‍💻",
        "name": "서버 장애 보고",
        "nameVi": "Báo cáo sự cố server",
        "difficulty": "Trung cấp",
        "domain": "Backend",
        "missionVi": "Báo cáo sự cố DB timeout và đề xuất phương án rollback.",
        "persona": "Kim Tech Lead",
        "role": "Team Leader",
        "company": "Naver",
        "color": const Color(0xFFF87171),
      },
      {
        "id": "s2",
        "avatar": "👩🏻‍💼",
        "name": "요구사항 리뷰",
        "nameVi": "Review yêu cầu tính năng",
        "difficulty": "Sơ cấp",
        "domain": "Frontend",
        "missionVi": "Làm rõ yêu cầu về UI animation với PO.",
        "persona": "Lee PO",
        "role": "Product Owner",
        "company": "Toss",
        "color": const Color(0xFF34D399),
      }
    ];

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Chọn tình huống'),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(
              'CHỌN TÌNH HUỐNG LUYỆN TẬP',
              style: GoogleFonts.jetBrainsMono(
                fontSize: 10,
                color: AppTheme.textSecondary,
                letterSpacing: 1,
              ),
            ),
            const SizedBox(height: 14),
            ...scenarios.map((sc) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: InkWell(
                  onTap: () {
                    context.push('/techtalk-chat', extra: sc);
                  },
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: AppTheme.surface.withOpacity(0.5),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: sc["color"].withOpacity(0.3),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: BoxDecoration(
                                color: sc["color"].withOpacity(0.18),
                                borderRadius: BorderRadius.circular(14),
                              ),
                              alignment: Alignment.center,
                              child: Text(
                                sc["avatar"],
                                style: const TextStyle(fontSize: 22),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    sc["name"],
                                    style: GoogleFonts.outfit(
                                      fontWeight: FontWeight.w700,
                                      fontSize: 16,
                                      color: AppTheme.textPrimary,
                                    ),
                                  ),
                                  Text(
                                    sc["nameVi"],
                                    style: GoogleFonts.inter(
                                      fontSize: 12,
                                      color: AppTheme.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                _buildBadge(sc["difficulty"], sc["color"]),
                                const SizedBox(height: 4),
                                _buildBadge(sc["domain"], AppTheme.textSecondary),
                              ],
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          decoration: BoxDecoration(
                            color: AppTheme.surface.withOpacity(0.8),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '🎯 ${sc["missionVi"]}',
                            style: GoogleFonts.inter(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Text(sc["avatar"], style: const TextStyle(fontSize: 14)),
                            const SizedBox(width: 8),
                            Text(
                              '${sc["persona"]} · ${sc["role"]} @ ${sc["company"]}',
                              style: GoogleFonts.inter(
                                fontSize: 11,
                                color: AppTheme.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
          ],
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

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class VideoScreen extends StatefulWidget {
  const VideoScreen({super.key});

  @override
  State<VideoScreen> createState() => _VideoScreenState();
}

class _VideoScreenState extends State<VideoScreen> {
  bool _playing = false;
  int _subIdx = 1;
  String _speed = "1×";

  final List<String> _vocabList = ["서버리스", "아키텍처", "배포", "자동화", "오류 처리", "함수"];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Video Lab'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: Container(
                width: double.infinity,
                color: const Color(0xFF050D1A),
                child: Stack(
                children: [
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            color: AppTheme.primary.withOpacity(0.18),
                            shape: BoxShape.circle,
                            border: Border.all(color: AppTheme.primary.withOpacity(0.44), width: 2),
                          ),
                          alignment: Alignment.center,
                          child: Icon(Icons.play_arrow, color: AppTheme.primary, size: 24),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'DEVIEW 2025 · Serverless Architecture',
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 10,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Positioned(
                    bottom: 8,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.75),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Column(
                          children: [
                            Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                _buildSubToken('새'),
                                _buildSubToken('버전을'),
                                _buildSubToken('배포했습니다'),
                              ],
                            ),
                            const SizedBox(height: 3),
                            Text(
                              'Đã triển khai phiên bản mới',
                              style: GoogleFonts.inter(
                                fontSize: 10,
                                color: Colors.white.withOpacity(0.6),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
            
            // Controls
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                border: Border(bottom: BorderSide(color: AppTheme.textSecondary.withOpacity(0.2))),
              ),
              child: Column(
                children: [
                  // Timeline
                  Container(
                    height: 6,
                    margin: const EdgeInsets.only(bottom: 10),
                    decoration: BoxDecoration(
                      color: AppTheme.textSecondary.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(3),
                    ),
                    child: Stack(
                      children: [
                        FractionallySizedBox(
                          alignment: Alignment.centerLeft,
                          widthFactor: 0.32,
                          child: Container(
                            decoration: BoxDecoration(
                              color: AppTheme.primary,
                              borderRadius: BorderRadius.circular(3),
                            ),
                          ),
                        ),
                        Positioned(
                          left: MediaQuery.of(context).size.width * 0.35,
                          child: Container(
                            width: 12,
                            height: 12,
                            decoration: BoxDecoration(
                              color: AppTheme.secondary,
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.black, width: 2),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  // Buttons
                  Row(
                    children: [
                      GestureDetector(
                        onTap: () => setState(() => _playing = !_playing),
                        child: Container(
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(color: AppTheme.primary, shape: BoxShape.circle),
                          child: Icon(
                            _playing ? Icons.pause : Icons.play_arrow,
                            size: 18,
                            color: Colors.black,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          '6:24 / 20:00',
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 10,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ),
                      Row(
                        children: ["0.75×", "1×", "1.25×"].map((s) {
                          final isSel = _speed == s;
                          return GestureDetector(
                            onTap: () => setState(() => _speed = s),
                            child: Container(
                              margin: const EdgeInsets.only(left: 4),
                              padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                              decoration: BoxDecoration(
                                color: isSel ? AppTheme.primary : AppTheme.textSecondary.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(5),
                              ),
                              child: Text(
                                s,
                                style: GoogleFonts.jetBrainsMono(
                                  fontSize: 9,
                                  color: isSel ? Colors.black : AppTheme.textSecondary,
                                ),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {},
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.surface.withOpacity(0.8),
                            foregroundColor: AppTheme.textSecondary,
                            elevation: 0,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                          ),
                          child: Text('← Trước', style: GoogleFonts.jetBrainsMono(fontSize: 11)),
                        ),
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {},
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.surface.withOpacity(0.8),
                            foregroundColor: AppTheme.textSecondary,
                            elevation: 0,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                          ),
                          child: Text('Tiếp →', style: GoogleFonts.jetBrainsMono(fontSize: 11)),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            
            // Vocab List
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(14),
                children: [
                  Text(
                    'TỪ VỰNG ĐOẠN NÀY',
                    style: GoogleFonts.jetBrainsMono(
                      fontSize: 10,
                      color: AppTheme.textSecondary,
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _vocabList.map((w) {
                      return GestureDetector(
                        onTap: () {
                          // TODO: Show dictionary bottom sheet
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: AppTheme.primary.withOpacity(0.12),
                            border: Border.all(color: AppTheme.primary.withOpacity(0.3)),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            w,
                            style: GoogleFonts.outfit(
                              fontSize: 13,
                              color: AppTheme.primary,
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSubToken(String word) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 2),
      child: Text(
        word,
        style: GoogleFonts.outfit(
          fontWeight: FontWeight.w600,
          fontSize: 14,
          color: Colors.white,
          decoration: TextDecoration.underline,
          decorationStyle: TextDecorationStyle.dotted,
          decorationColor: Colors.white.withOpacity(0.5),
        ),
      ),
    );
  }
}

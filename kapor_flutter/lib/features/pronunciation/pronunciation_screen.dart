import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class PronunciationScreen extends StatefulWidget {
  const PronunciationScreen({super.key});

  @override
  State<PronunciationScreen> createState() => _PronunciationScreenState();
}

class _PronunciationScreenState extends State<PronunciationScreen> {
  bool _recording = false;
  String _phase = "listen"; // listen, record, result

  void _handleRecord() {
    if (_recording) {
      setState(() {
        _recording = false;
      });
      Future.delayed(const Duration(milliseconds: 600), () {
        if (mounted) setState(() => _phase = "result");
      });
    } else {
      setState(() {
        _recording = true;
        _phase = "record";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Pronunciation Lab'),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Sample Sentence
            Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'CÂU MẪU',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                        letterSpacing: 1,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '서버 배포가 완료되었습니다',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w700,
                        fontSize: 20,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Việc triển khai server đã hoàn tất',
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Container(
                          width: 34,
                          height: 34,
                          decoration: BoxDecoration(
                            color: AppTheme.primary.withOpacity(0.18),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(Icons.play_arrow, size: 16, color: AppTheme.primary),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Row(
                            children: List.generate(10, (i) {
                              return Expanded(
                                child: Container(
                                  height: (i % 3 + 1) * 6.0,
                                  margin: const EdgeInsets.symmetric(horizontal: 1),
                                  decoration: BoxDecoration(
                                    color: AppTheme.primary.withOpacity(0.65),
                                    borderRadius: BorderRadius.circular(2),
                                  ),
                                ),
                              );
                            }),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // Recording Area
            Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'BẢN GHI ÂM CỦA BẠN',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                        letterSpacing: 1,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      height: 48,
                      alignment: Alignment.bottomCenter,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: List.generate(20, (i) {
                          return Expanded(
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 100),
                              height: _recording ? (i % 4 + 1) * 8.0 + 5.0 : (_phase != "listen" ? (i % 2 + 1) * 6.0 : 4.0),
                              margin: const EdgeInsets.symmetric(horizontal: 1),
                              decoration: BoxDecoration(
                                color: _recording
                                    ? Colors.red[300]
                                    : (_phase != "listen" ? AppTheme.secondary : AppTheme.textSecondary.withOpacity(0.3)),
                                borderRadius: BorderRadius.circular(2),
                              ),
                            ),
                          );
                        }),
                      ),
                    ),
                    const SizedBox(height: 14),
                    ElevatedButton(
                      onPressed: _handleRecord,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _recording ? Colors.red[300]!.withOpacity(0.12) : AppTheme.primary.withOpacity(0.12),
                        foregroundColor: _recording ? Colors.red[300] : AppTheme.primary,
                        side: BorderSide(
                          color: (_recording ? Colors.red[300]! : AppTheme.primary).withOpacity(0.4),
                        ),
                        elevation: 0,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.mic, size: 16),
                          const SizedBox(width: 8),
                          Text(
                            _recording ? 'Dừng ghi âm' : (_phase == "result" ? 'Ghi âm lại' : 'Bắt đầu ghi âm'),
                            style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // Result Phase
            if (_phase == "result") ...[
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text(
                  'ĐÁNH GIÁ TỪNG TỪ',
                  style: GoogleFonts.jetBrainsMono(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                    letterSpacing: 1,
                  ),
                ),
              ),
              Wrap(
                spacing: 10,
                children: [
                  _buildWordEval('서버', const Color(0xFF34D399)),
                  _buildWordEval('배포가', const Color(0xFF34D399)),
                  _buildWordEval('완료', AppTheme.secondary),
                  _buildWordEval('되었습니다', Colors.red[300]!),
                ],
              ),
              const SizedBox(height: 12),
              Card(
                margin: const EdgeInsets.only(bottom: 16),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'ĐIỂM SỐ',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                          letterSpacing: 1,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          _buildScoreCircle(82, 'ACCURACY', AppTheme.primary),
                          _buildScoreCircle(75, 'FLUENCY', const Color(0xFFA78BFA)),
                          _buildScoreCircle(68, 'PROSODY', AppTheme.secondary),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              ElevatedButton(
                onPressed: () {},
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: Text(
                  'Bài tiếp theo →',
                  style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 14),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildWordEval(String text, Color color) {
    return Container(
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: color.withOpacity(0.5), width: 2)),
      ),
      child: Text(
        text,
        style: GoogleFonts.outfit(
          fontWeight: FontWeight.w700,
          fontSize: 18,
          color: color,
        ),
      ),
    );
  }

  Widget _buildScoreCircle(int score, String label, Color color) {
    return Column(
      children: [
        SizedBox(
          width: 60,
          height: 60,
          child: Stack(
            children: [
              Center(
                child: CircularProgressIndicator(
                  value: score / 100.0,
                  strokeWidth: 6,
                  backgroundColor: AppTheme.textSecondary.withOpacity(0.2),
                  color: color,
                ),
              ),
              Center(
                child: Text(
                  score.toString(),
                  style: GoogleFonts.jetBrainsMono(
                    fontWeight: FontWeight.w700,
                    fontSize: 16,
                    color: AppTheme.textPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          label,
          style: GoogleFonts.jetBrainsMono(
            fontSize: 9,
            color: color,
          ),
        ),
      ],
    );
  }
}

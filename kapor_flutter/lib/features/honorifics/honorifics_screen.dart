import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class HonorificsScreen extends StatefulWidget {
  const HonorificsScreen({super.key});

  @override
  State<HonorificsScreen> createState() => _HonorificsScreenState();
}

class _HonorificsScreenState extends State<HonorificsScreen> {
  final TextEditingController _controller = TextEditingController(text: "나 오늘 서버 배포 했어. 너 확인해봐.");
  bool _analyzed = false;
  bool _copied = false;
  int? _activeIdx;
  
  final String _formalText = "저 오늘 서버 배포 하였습니다. 귀하 확인해 주시기 바랍니다.";
  
  final List<Map<String, String>> _corrections = [
    {
      "original": "나",
      "corrected": "저",
      "type": "Đại từ",
      "explanation": "Dùng '저' thay cho '나' để xưng hô khiêm nhường."
    },
    {
      "original": "했어",
      "corrected": "하였습니다",
      "type": "Đuôi câu",
      "explanation": "Chuyển từ 반말 sang dạng trang trọng 하십시오체."
    },
    {
      "original": "너",
      "corrected": "귀하",
      "type": "Đại từ",
      "explanation": "Dùng '귀하' thay cho '너' lịch sự hơn."
    },
    {
      "original": "확인해봐",
      "corrected": "확인해 주시기 바랍니다",
      "type": "Đuôi câu",
      "explanation": "Chuyển yêu cầu thành dạng đề nghị lịch sự."
    }
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
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
        title: const Text('Honorifics Analyzer'),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Input Area
            Container(
              decoration: BoxDecoration(
                color: AppTheme.surface.withOpacity(0.5),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.textSecondary.withOpacity(0.3)),
              ),
              child: TextField(
                controller: _controller,
                onChanged: (_) => setState(() => _analyzed = false),
                maxLines: 3,
                style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary, height: 1.6),
                decoration: const InputDecoration(
                  contentPadding: EdgeInsets.all(12),
                  border: InputBorder.none,
                ),
              ),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: () => setState(() => _analyzed = true),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primary,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.flash_on, size: 16),
                  const SizedBox(width: 8),
                  Text(
                    '분석하기 (Phân tích)',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 14),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            
            // Results Area
            if (_analyzed) ...[
              // Politeness Level
              Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'MỨC ĐỘ LỊCH SỰ',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                          letterSpacing: 1,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.red[300]!.withOpacity(0.2),
                              border: Border.all(color: Colors.red[300]!.withOpacity(0.5)),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              '반말 (Casual)',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13, color: Colors.red[300]),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Icon(Icons.arrow_forward, size: 14, color: AppTheme.textSecondary),
                          const SizedBox(width: 10),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                            decoration: BoxDecoration(
                              color: const Color(0xFF34D399).withOpacity(0.2),
                              border: Border.all(color: const Color(0xFF34D399).withOpacity(0.5)),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              '하십시오체',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13, color: const Color(0xFF34D399)),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),

              // Corrections
              Text(
                'SỬA ĐỔI (${_corrections.length})',
                style: GoogleFonts.jetBrainsMono(
                  fontSize: 10,
                  color: AppTheme.textSecondary,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 8),
              ...List.generate(_corrections.length, (i) {
                final c = _corrections[i];
                final isActive = _activeIdx == i;
                return GestureDetector(
                  onTap: () => setState(() => _activeIdx = isActive ? null : i),
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 6),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.surface.withOpacity(0.5),
                      border: Border.all(
                        color: isActive ? AppTheme.primary.withOpacity(0.5) : AppTheme.textSecondary.withOpacity(0.2),
                      ),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                Text(
                                  c["original"]!,
                                  style: GoogleFonts.outfit(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 14,
                                    color: Colors.red[300],
                                    decoration: TextDecoration.lineThrough,
                                    decorationColor: Colors.red[300],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Icon(Icons.arrow_forward, size: 12, color: AppTheme.textSecondary),
                                const SizedBox(width: 8),
                                Text(
                                  c["corrected"]!,
                                  style: GoogleFonts.outfit(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 14,
                                    color: const Color(0xFF34D399),
                                  ),
                                ),
                              ],
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: AppTheme.primary.withOpacity(0.15),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                c["type"]!,
                                style: GoogleFonts.jetBrainsMono(fontSize: 9, color: AppTheme.primary),
                              ),
                            ),
                          ],
                        ),
                        if (isActive)
                          Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: Text(
                              c["explanation"]!,
                              style: GoogleFonts.inter(
                                fontSize: 11,
                                color: AppTheme.textSecondary.withOpacity(0.8),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              }),
              const SizedBox(height: 6),

              // Final Result
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'KẾT QUẢ CHÍNH THỨC',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                          letterSpacing: 1,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _formalText,
                        style: GoogleFonts.inter(
                          fontSize: 14,
                          color: const Color(0xFF34D399),
                          height: 1.7,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton(
                              onPressed: () {
                                setState(() {
                                  _controller.text = _formalText;
                                  _analyzed = false;
                                });
                              },
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppTheme.primary,
                                side: BorderSide(color: AppTheme.primary.withOpacity(0.4)),
                                backgroundColor: AppTheme.primary.withOpacity(0.12),
                                padding: const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.check, size: 14),
                                  const SizedBox(width: 6),
                                  Text(
                                    'Áp dụng',
                                    style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: OutlinedButton(
                              onPressed: () {
                                setState(() => _copied = true);
                                Future.delayed(const Duration(seconds: 2), () {
                                  if (mounted) setState(() => _copied = false);
                                });
                              },
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppTheme.textSecondary,
                                side: BorderSide(color: AppTheme.textSecondary.withOpacity(0.3)),
                                backgroundColor: AppTheme.surface.withOpacity(0.5),
                                padding: const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(_copied ? Icons.check : Icons.copy, size: 14),
                                  const SizedBox(width: 6),
                                  Text(
                                    _copied ? 'Đã sao chép!' : 'Sao chép',
                                    style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

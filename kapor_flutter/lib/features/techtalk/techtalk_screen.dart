import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class TechTalkScreen extends StatefulWidget {
  final Map<String, dynamic>? scenario;

  const TechTalkScreen({super.key, this.scenario});

  @override
  State<TechTalkScreen> createState() => _TechTalkScreenState();
}

class _TechTalkScreenState extends State<TechTalkScreen> {
  final TextEditingController _inputController = TextEditingController();
  final List<Map<String, dynamic>> _messages = [
    {
      "id": "1",
      "role": "ai",
      "content": "안녕하세요. 현재 서버 상태에 대해 보고해 주시겠어요?",
      "eval": null
    }
  ];
  bool _showHint = false;
  bool _voiceMode = false;

  void _send() {
    if (_inputController.text.trim().isEmpty) return;
    
    setState(() {
      _messages.add({
        "id": DateTime.now().millisecondsSinceEpoch.toString(),
        "role": "user",
        "content": _inputController.text,
        "eval": {"grammar": 82, "vocabulary": 78, "politeness": 88}
      });
      _inputController.clear();
    });

    Future.delayed(const Duration(seconds: 1), () {
      if (mounted) {
        setState(() {
          _messages.add({
            "id": DateTime.now().millisecondsSinceEpoch.toString(),
            "role": "ai",
            "content": "알겠습니다. 장애 발생 시간과 영향 범위를 구체적으로 보고해 주세요.",
            "eval": null
          });
        });
      }
    });
  }

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Default fallback scenario if none passed
    final sc = widget.scenario ?? {
      "avatar": "👨🏻‍💻",
      "nameVi": "Báo cáo sự cố server",
      "difficulty": "Trung cấp",
      "missionVi": "Báo cáo sự cố DB timeout và đề xuất phương án rollback.",
      "persona": "Kim Tech Lead",
      "role": "Team Leader",
      "company": "Naver",
      "color": const Color(0xFFF87171),
    };

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(color: AppTheme.textSecondary.withOpacity(0.2)),
                ),
              ),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Text(
                      'TechTalk AI',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
                  ),
                  OutlinedButton(
                    onPressed: () {
                      context.push('/techtalk-result');
                    },
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red[300],
                      side: BorderSide(color: Colors.red[300]!.withOpacity(0.4)),
                      backgroundColor: Colors.red[300]!.withOpacity(0.1),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
                    ),
                    child: Text(
                      'Kết thúc',
                      style: GoogleFonts.outfit(fontWeight: FontWeight.w600, fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            
            // Sub-header (Scenario Info)
            Container(
              padding: const EdgeInsets.all(14),
              color: AppTheme.surface.withOpacity(0.5),
              child: Column(
                children: [
                  Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: sc["color"].withOpacity(0.18),
                          shape: BoxShape.circle,
                          border: Border.all(color: sc["color"].withOpacity(0.44), width: 2),
                        ),
                        alignment: Alignment.center,
                        child: Text(sc["avatar"], style: const TextStyle(fontSize: 18)),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              sc["persona"],
                              style: GoogleFonts.outfit(
                                fontWeight: FontWeight.w600,
                                fontSize: 14,
                                color: AppTheme.textPrimary,
                              ),
                            ),
                            Row(
                              children: [
                                _buildBadge(sc["role"], sc["color"]),
                                const SizedBox(width: 6),
                                Text(
                                  sc["company"],
                                  style: GoogleFonts.jetBrainsMono(
                                    fontSize: 10,
                                    color: AppTheme.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      _buildBadge(sc["difficulty"], AppTheme.secondary),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppTheme.surface.withOpacity(0.8),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '🎯 ${sc["missionVi"]}',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            // Chat List
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.all(14),
                itemCount: _messages.length,
                itemBuilder: (context, index) {
                  final msg = _messages[index];
                  final isUser = msg["role"] == "user";
                  return Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        maxWidth: MediaQuery.of(context).size.width * 0.78,
                      ),
                      child: Column(
                        crossAxisAlignment: isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                            decoration: BoxDecoration(
                              color: isUser ? AppTheme.primary : AppTheme.surface,
                              borderRadius: BorderRadius.only(
                                topLeft: const Radius.circular(16),
                                topRight: const Radius.circular(16),
                                bottomLeft: Radius.circular(isUser ? 16 : 4),
                                bottomRight: Radius.circular(isUser ? 4 : 16),
                              ),
                            ),
                            child: Text(
                              msg["content"],
                              style: GoogleFonts.inter(
                                fontSize: 13,
                                color: isUser ? Colors.black : AppTheme.textPrimary,
                                height: 1.5,
                              ),
                            ),
                          ),
                          if (msg["eval"] != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 4.0),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  _buildEvalStat("G", msg["eval"]["grammar"], const Color(0xFF34D399)),
                                  const SizedBox(width: 8),
                                  _buildEvalStat("V", msg["eval"]["vocabulary"], const Color(0xFF60A5FA)),
                                  const SizedBox(width: 8),
                                  _buildEvalStat("P", msg["eval"]["politeness"], const Color(0xFFA78BFA)),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
            
            // Hint Box
            if (_showHint)
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                decoration: BoxDecoration(
                  color: AppTheme.secondary.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.secondary.withOpacity(0.3)),
                ),
                width: double.infinity,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '💡 GỢI Ý',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.secondary,
                        letterSpacing: 1,
                      ),
                    ),
                    const SizedBox(height: 5),
                    RichText(
                      text: TextSpan(
                        style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textPrimary),
                        children: [
                          const TextSpan(text: 'Từ khóa: '),
                          TextSpan(
                            text: '배포, 서버, 장애',
                            style: TextStyle(color: AppTheme.primary, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'Mẫu câu: "...에 문제가 발생했습니다"',
                      style: GoogleFonts.inter(fontSize: 11, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
            
            // Bottom Input
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(
                border: Border(top: BorderSide(color: AppTheme.textSecondary.withOpacity(0.2))),
              ),
              child: Row(
                children: [
                  GestureDetector(
                    onTap: () => setState(() => _showHint = !_showHint),
                    child: Container(
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(
                        color: AppTheme.secondary.withOpacity(0.18),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(Icons.lightbulb_outline, size: 16, color: AppTheme.secondary),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _voiceMode
                        ? Container(
                            height: 38,
                            decoration: BoxDecoration(
                              color: AppTheme.surface.withOpacity(0.5),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: Colors.red[300]!.withOpacity(0.3)),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.radio_button_checked, size: 14, color: Colors.red[300]),
                                const SizedBox(width: 6),
                                Text(
                                  'Đang ghi âm...',
                                  style: GoogleFonts.jetBrainsMono(
                                    fontSize: 11,
                                    color: Colors.red[300],
                                  ),
                                ),
                              ],
                            ),
                          )
                        : SizedBox(
                            height: 38,
                            child: TextField(
                              controller: _inputController,
                              style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
                              decoration: InputDecoration(
                                hintText: 'Nhập câu tiếng Hàn...',
                                hintStyle: TextStyle(color: AppTheme.textSecondary),
                                contentPadding: const EdgeInsets.symmetric(horizontal: 12),
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: BorderSide(color: AppTheme.textSecondary.withOpacity(0.3)),
                                ),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: BorderSide(color: AppTheme.textSecondary.withOpacity(0.3)),
                                ),
                                filled: true,
                                fillColor: AppTheme.surface.withOpacity(0.5),
                              ),
                              onSubmitted: (_) => _send(),
                            ),
                          ),
                  ),
                  const SizedBox(width: 8),
                  GestureDetector(
                    onTap: () => setState(() => _voiceMode = !_voiceMode),
                    child: Container(
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(
                        color: _voiceMode ? Colors.red[300]!.withOpacity(0.18) : AppTheme.textSecondary.withOpacity(0.2),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(Icons.mic, size: 16, color: _voiceMode ? Colors.red[300] : AppTheme.textSecondary),
                    ),
                  ),
                  const SizedBox(width: 8),
                  GestureDetector(
                    onTap: _send,
                    child: Container(
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(
                        color: AppTheme.primary,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.send, size: 14, color: Colors.black),
                    ),
                  ),
                ],
              ),
            ),
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

  Widget _buildEvalStat(String key, int value, Color color) {
    return Text(
      '$key:$value',
      style: GoogleFonts.jetBrainsMono(
        fontSize: 9,
        color: color,
      ),
    );
  }
}

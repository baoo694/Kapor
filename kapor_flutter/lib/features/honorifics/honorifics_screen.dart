import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/honorifics_service.dart';

class HonorificsScreen extends StatefulWidget {
  const HonorificsScreen({super.key});

  @override
  State<HonorificsScreen> createState() => _HonorificsScreenState();
}

class _HonorificsScreenState extends State<HonorificsScreen> {
  final _controller = TextEditingController(text: '나 오늘 서버 배포 했어. 너 확인해봐.');
  final _service = HonorificsService();
  HonorificAnalysis? _analysis;
  String? _error;
  bool _loading = false;
  int? _expandedIndex;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _analyze() async {
    final text = _controller.text.trim();
    if (text.isEmpty) {
      setState(() => _error = 'Hãy nhập văn bản tiếng Hàn cần kiểm tra.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _analysis = null;
    });
    try {
      final result = await _service.analyze(text);
      if (mounted) setState(() => _analysis = result);
    } catch (error) {
      if (mounted)
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final analysis = _analysis;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: context.pop,
        ),
        title: const Text('Honorifics Analyzer'),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(
              'KIỂM TRA KÍNH NGỮ',
              style: GoogleFonts.jetBrainsMono(
                fontSize: 10,
                color: AppTheme.textSecondary,
                letterSpacing: 1,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _controller,
              maxLines: 5,
              maxLength: 1500,
              onChanged: (_) {
                if (_analysis != null || _error != null)
                  setState(() {
                    _analysis = null;
                    _error = null;
                  });
              },
              style: GoogleFonts.inter(
                color: AppTheme.textPrimary,
                height: 1.55,
              ),
              decoration: InputDecoration(
                hintText: 'Nhập văn bản tiếng Hàn cần kiểm tra...',
                hintStyle: GoogleFonts.inter(color: AppTheme.textSecondary),
                filled: true,
                fillColor: AppTheme.surface,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            const SizedBox(height: 10),
            ElevatedButton.icon(
              onPressed: _loading ? null : _analyze,
              icon: _loading
                  ? const SizedBox.square(
                      dimension: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.black,
                      ),
                    )
                  : const Icon(Icons.auto_awesome, size: 17),
              label: Text(
                _loading ? 'Đang phân tích...' : '분석하기 (Phân tích)',
                style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primary,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
            ),
            if (_error != null) _messageCard(_error!, Colors.redAccent),
            if (analysis != null) ...[_result(analysis)],
          ],
        ),
      ),
    );
  }

  Widget _result(HonorificAnalysis analysis) {
    final level = analysis.currentLevel;
    final color = level == 'hasipsio'
        ? const Color(0xFF34D399)
        : level == 'heyohaet'
        ? AppTheme.secondary
        : Colors.redAccent;
    final label = level == 'hasipsio'
        ? '하십시오체 · Corporate'
        : level == 'heyohaet'
        ? '해요체 · Standard'
        : '반말 · Casual';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 16),
        Card(
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
                const SizedBox(height: 10),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: color.withOpacity(.16),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: color.withOpacity(.45)),
                      ),
                      child: Text(
                        label,
                        style: GoogleFonts.outfit(
                          color: color,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      '${(analysis.confidence * 100).round()}% tin cậy',
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text(
          'SỬA ĐỔI (${analysis.corrections.length})',
          style: GoogleFonts.jetBrainsMono(
            fontSize: 10,
            color: AppTheme.textSecondary,
            letterSpacing: 1,
          ),
        ),
        const SizedBox(height: 8),
        if (analysis.corrections.isEmpty)
          _messageCard(
            'Không phát hiện lỗi kính ngữ phổ biến.',
            AppTheme.primary,
          ),
        ...List.generate(
          analysis.corrections.length,
          (index) => _diffCard(analysis.corrections[index], index),
        ),
        const SizedBox(height: 10),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'BẢN ĐỀ XUẤT',
                  style: GoogleFonts.jetBrainsMono(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                    letterSpacing: 1,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  analysis.transformedText,
                  style: GoogleFonts.inter(
                    color: const Color(0xFF34D399),
                    height: 1.55,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => setState(() {
                          _controller.text = analysis.transformedText;
                          _analysis = null;
                        }),
                        icon: const Icon(Icons.check, size: 16),
                        label: const Text('Áp dụng'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () async {
                          await Clipboard.setData(
                            ClipboardData(text: analysis.transformedText),
                          );
                          if (mounted)
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('Đã sao chép văn bản.'),
                              ),
                            );
                        },
                        icon: const Icon(Icons.copy, size: 16),
                        label: const Text('Sao chép'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _diffCard(CorrectionDiff diff, int index) {
    final isExpanded = _expandedIndex == index;
    return Card(
      child: InkWell(
        onTap: () => setState(() => _expandedIndex = isExpanded ? null : index),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    diff.original,
                    style: GoogleFonts.outfit(
                      color: Colors.redAccent,
                      fontWeight: FontWeight.w700,
                      decoration: TextDecoration.lineThrough,
                    ),
                  ),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 8),
                    child: Icon(Icons.arrow_forward, size: 15),
                  ),
                  Text(
                    diff.corrected,
                    style: GoogleFonts.outfit(
                      color: const Color(0xFF34D399),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    diff.type,
                    style: GoogleFonts.jetBrainsMono(
                      fontSize: 9,
                      color: AppTheme.primary,
                    ),
                  ),
                ],
              ),
              if (isExpanded)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    diff.explanation,
                    style: GoogleFonts.inter(
                      fontSize: 12,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _messageCard(String text, Color color) => Padding(
    padding: const EdgeInsets.only(top: 12),
    child: Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withOpacity(.1),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text(text, style: GoogleFonts.inter(color: color, fontSize: 12)),
    ),
  );
}

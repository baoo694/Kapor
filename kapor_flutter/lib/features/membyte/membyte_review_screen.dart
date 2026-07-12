import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:math';

import '../../core/theme/app_theme.dart';

class MemByteReviewScreen extends StatefulWidget {
  final String? deckId;
  const MemByteReviewScreen({super.key, this.deckId});

  @override
  State<MemByteReviewScreen> createState() => _MemByteReviewScreenState();
}

class _MemByteReviewScreenState extends State<MemByteReviewScreen> with SingleTickerProviderStateMixin {
  int _idx = 0;
  bool _flipped = false;
  late AnimationController _animationController;
  late Animation<double> _animation;

  // Mock data
  final List<Map<String, String>> _flashcards = [
    {
      "korean": "그리드",
      "pronunciation": "geu-ri-deu",
      "vietnamese": "Lưới (Grid)",
      "itContext": "Hệ thống bố cục 2 chiều trong CSS.",
      "example": "그리드 레이아웃을 사용하세요.",
      "code": "display: grid;\ngrid-template-columns: 1fr 1fr;"
    },
    {
      "korean": "변수",
      "pronunciation": "byeon-su",
      "vietnamese": "Biến",
      "itContext": "Nơi lưu trữ dữ liệu trong bộ nhớ.",
      "example": "변수를 선언합니다.",
      "code": "let myVar = 10;"
    }
  ];

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _animation = Tween<double>(begin: 0, end: 1).animate(_animationController);
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  void _flipCard() {
    if (_flipped) {
      _animationController.reverse();
    } else {
      _animationController.forward();
    }
    setState(() {
      _flipped = !_flipped;
    });
  }

  void _rate() {
    _flipCard(); // Unflip first
    Future.delayed(const Duration(milliseconds: 200), () {
      setState(() {
        _idx++;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final card = _flashcards[_idx % _flashcards.length];
    final remaining = 15 - (_idx % 15);
    final progress = (_idx % 15) / 15.0;

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text('Ôn tập · $remaining còn lại'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Top Progress Bar
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
              child: Container(
                height: 4,
                width: double.infinity,
                decoration: BoxDecoration(
                  color: AppTheme.textSecondary.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: FractionallySizedBox(
                  alignment: Alignment.centerLeft,
                  widthFactor: progress,
                  child: Container(
                    decoration: BoxDecoration(
                      color: AppTheme.primary,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),
            ),

            // Flashcard
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    GestureDetector(
                      onTap: _flipCard,
                      child: AnimatedBuilder(
                        animation: _animation,
                        builder: (context, child) {
                          final angle = _animation.value * pi;
                          final isFront = angle < pi / 2;

                          return Transform(
                            transform: Matrix4.identity()
                              ..setEntry(3, 2, 0.001)
                              ..rotateY(angle),
                            alignment: Alignment.center,
                            child: SizedBox(
                              width: double.infinity,
                              height: 280, // Taller for more content
                              child: isFront
                                  ? _buildFront(card)
                                  : Transform(
                                      transform: Matrix4.identity()..rotateY(pi),
                                      alignment: Alignment.center,
                                      child: _buildBack(card),
                                    ),
                            ),
                          );
                        },
                      ),
                    ),
                    const SizedBox(height: 30),
                    
                    // Bottom Controls
                    if (_flipped)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          _buildRatingBtn("Again", const Color(0xFFF87171), "< 1m"),
                          _buildRatingBtn("Hard", const Color(0xFFFB923C), "6m"),
                          _buildRatingBtn("Good", const Color(0xFF34D399), "1d"),
                          _buildRatingBtn("Easy", const Color(0xFF60A5FA), "4d"),
                        ],
                      )
                    else
                      ElevatedButton(
                        onPressed: _flipCard,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primary,
                          foregroundColor: Colors.black,
                          minimumSize: const Size(double.infinity, 50),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: Text(
                          'Xem đáp án',
                          style: GoogleFonts.outfit(
                            fontWeight: FontWeight.w700,
                            fontSize: 16,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFront(Map<String, String> card) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface.withOpacity(0.8),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.primary.withOpacity(0.3)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            card["korean"]!,
            style: GoogleFonts.outfit(
              fontWeight: FontWeight.w800,
              fontSize: 52,
              color: AppTheme.primary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '/${card["pronunciation"]}/',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 14,
              color: AppTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 16),
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: AppTheme.primary.withOpacity(0.18),
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: Icon(Icons.volume_up, size: 18, color: AppTheme.primary),
              onPressed: () {},
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Nhấn để lật thẻ',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 10,
              color: AppTheme.textSecondary.withOpacity(0.7),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBack(Map<String, String> card) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.surface.withOpacity(0.8),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.primary.withOpacity(0.3)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            card["vietnamese"]!,
            style: GoogleFonts.outfit(
              fontWeight: FontWeight.w700,
              fontSize: 24,
              color: AppTheme.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            card["itContext"]!,
            style: GoogleFonts.inter(
              fontSize: 12,
              color: AppTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            card["example"]!,
            style: GoogleFonts.inter(
              fontSize: 12,
              fontStyle: FontStyle.italic,
              color: AppTheme.textSecondary.withOpacity(0.8),
            ),
          ),
          const SizedBox(height: 16),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppTheme.surface.withOpacity(0.5),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: AppTheme.textSecondary.withOpacity(0.2),
              ),
            ),
            child: Text(
              card["code"]!,
              style: GoogleFonts.jetBrainsMono(
                fontSize: 10,
                color: AppTheme.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRatingBtn(String label, Color color, String time) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4.0),
        child: InkWell(
          onTap: _rate,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: color.withOpacity(0.4)),
            ),
            child: Column(
              children: [
                Text(
                  label,
                  style: GoogleFonts.outfit(
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                    color: color,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  time,
                  style: GoogleFonts.jetBrainsMono(
                    fontSize: 9,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

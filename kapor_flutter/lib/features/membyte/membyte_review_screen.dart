import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/membyte_service.dart';

class MemByteReviewScreen extends StatefulWidget {
  final String? deckId;

  const MemByteReviewScreen({super.key, this.deckId});

  @override
  State<MemByteReviewScreen> createState() => _MemByteReviewScreenState();
}

class _MemByteReviewScreenState extends State<MemByteReviewScreen>
    with SingleTickerProviderStateMixin {
  final MemByteService _memByteService = MemByteService();
  final Stopwatch _responseTimer = Stopwatch();
  late final AnimationController _animationController;
  late final Animation<double> _animation;
  List<MemByteReviewCard> _flashcards = const [];
  int _reviewedCount = 0;
  bool _flipped = false;
  bool _isLoading = true;
  bool _isRating = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _animation = Tween<double>(begin: 0, end: 1).animate(_animationController);
    _loadSession();
  }

  @override
  void dispose() {
    _responseTimer.stop();
    _animationController.dispose();
    super.dispose();
  }

  Future<void> _loadSession() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final cards = await _memByteService.getReviewCards(
        deckId: widget.deckId,
        // A deck can introduce saved new cards. The global session stays strictly due-only.
        includeNew: widget.deckId != null && widget.deckId != 'all',
      );
      if (!mounted) return;
      setState(() {
        _flashcards = cards;
        _isLoading = false;
      });
      _responseTimer
        ..reset()
        ..start();
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _errorMessage = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  void _flipCard() {
    if (_isRating || _flashcards.isEmpty) return;
    if (_flipped) {
      _animationController.reverse();
    } else {
      _animationController.forward();
    }
    setState(() => _flipped = !_flipped);
  }

  Future<void> _rate(String rating) async {
    if (!_flipped || _isRating || _flashcards.isEmpty) return;
    final card = _flashcards.first;
    setState(() => _isRating = true);
    try {
      await _memByteService.rateCard(
        cardId: card.id,
        rating: rating,
        responseTimeMs: _responseTimer.elapsedMilliseconds,
      );
      if (!mounted) return;
      _animationController.reset();
      setState(() {
        _flashcards = _flashcards.skip(1).toList();
        _reviewedCount += 1;
        _flipped = false;
      });
      _responseTimer
        ..reset()
        ..start();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    } finally {
      if (mounted) setState(() => _isRating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_errorMessage != null) {
      return Scaffold(
        appBar: AppBar(),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Text(_errorMessage!, textAlign: TextAlign.center),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: _loadSession,
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }
    if (_flashcards.isEmpty) {
      return _ReviewCompleteScreen(reviewedCount: _reviewedCount);
    }

    final card = _flashcards.first;
    final total = _reviewedCount + _flashcards.length;
    final remaining = _flashcards.length;
    final progress = total == 0 ? 0.0 : _reviewedCount / total;

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
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Container(
                height: 4,
                width: double.infinity,
                decoration: BoxDecoration(
                  color: AppTheme.textSecondary.withValues(alpha: 0.2),
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
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
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
                              height: 360,
                              child: isFront
                                  ? _buildFront(card)
                                  : Transform(
                                      transform: Matrix4.identity()
                                        ..rotateY(pi),
                                      alignment: Alignment.center,
                                      child: _buildBack(card),
                                    ),
                            ),
                          );
                        },
                      ),
                    ),
                    const SizedBox(height: 30),
                    if (_flipped)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          _buildRatingBtn(
                            'Again',
                            'AGAIN',
                            const Color(0xFFF87171),
                            card,
                          ),
                          _buildRatingBtn(
                            'Hard',
                            'HARD',
                            const Color(0xFFFB923C),
                            card,
                          ),
                          _buildRatingBtn(
                            'Good',
                            'GOOD',
                            const Color(0xFF34D399),
                            card,
                          ),
                          _buildRatingBtn(
                            'Easy',
                            'EASY',
                            const Color(0xFF60A5FA),
                            card,
                          ),
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

  Widget _buildFront(MemByteReviewCard card) {
    return Container(
      decoration: _cardDecoration(),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            card.korean,
            style: GoogleFonts.outfit(
              fontWeight: FontWeight.w800,
              fontSize: 52,
              color: AppTheme.primary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '/${card.pronunciation}/',
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
              color: AppTheme.primary.withValues(alpha: 0.18),
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
              color: AppTheme.textSecondary.withValues(alpha: 0.7),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBack(MemByteReviewCard card) {
    final meaning = card.vietnamese.isEmpty ? card.english : card.vietnamese;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: SingleChildScrollView(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _detailLabel('NGHĨA'),
            const SizedBox(height: 6),
            Text(
              meaning,
              style: GoogleFonts.outfit(
                fontWeight: FontWeight.w700,
                fontSize: 24,
                color: AppTheme.textPrimary,
              ),
            ),
            if (card.definitionEn.isNotEmpty) ...[
              const SizedBox(height: 18),
              _detailLabel('ENGLISH DEFINITION'),
              const SizedBox(height: 6),
              Text(
                card.definitionEn,
                style: GoogleFonts.inter(
                  fontSize: 15,
                  height: 1.35,
                  color: AppTheme.textSecondary,
                ),
              ),
            ],
            if (card.exampleKo.isNotEmpty) ...[
              const SizedBox(height: 18),
              _detailLabel('VÍ DỤ TIẾNG HÀN'),
              const SizedBox(height: 6),
              Text(
                card.exampleKo,
                style: GoogleFonts.inter(
                  fontSize: 16,
                  height: 1.35,
                  fontStyle: FontStyle.italic,
                  color: AppTheme.textPrimary,
                ),
              ),
            ],
            if (card.grammarNote.isNotEmpty) ...[
              const SizedBox(height: 18),
              _detailLabel('GHI CHÚ NGỮ PHÁP'),
              const SizedBox(height: 6),
              Text(
                card.grammarNote,
                style: GoogleFonts.inter(
                  fontSize: 14,
                  height: 1.35,
                  color: AppTheme.textSecondary,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _detailLabel(String label) => Text(
    label,
    style: GoogleFonts.jetBrainsMono(
      color: AppTheme.primary,
      fontSize: 10,
      fontWeight: FontWeight.w700,
      letterSpacing: .8,
    ),
  );

  BoxDecoration _cardDecoration() => BoxDecoration(
    color: AppTheme.surface.withValues(alpha: 0.8),
    borderRadius: BorderRadius.circular(18),
    border: Border.all(color: AppTheme.primary.withValues(alpha: 0.3)),
  );

  Widget _buildRatingBtn(
    String label,
    String rating,
    Color color,
    MemByteReviewCard card,
  ) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: InkWell(
          onTap: _isRating ? null : () => _rate(rating),
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 10),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: color.withValues(alpha: 0.4)),
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
                  card.nextReviewTimes[rating] ?? '—',
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

class _ReviewCompleteScreen extends StatelessWidget {
  final int reviewedCount;

  const _ReviewCompleteScreen({required this.reviewedCount});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.check_circle_rounded,
                color: AppTheme.primary,
                size: 64,
              ),
              const SizedBox(height: 16),
              Text(
                reviewedCount == 0
                    ? 'Chưa có thẻ cần ôn'
                    : 'Đã hoàn thành phiên ôn tập',
                style: GoogleFonts.outfit(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                reviewedCount == 0
                    ? 'Hãy chọn một bộ thẻ để học các thẻ mới, hoặc quay lại sau khi có thẻ đến hạn.'
                    : 'FSRS đã cập nhật lịch ôn tiếp theo cho các thẻ của bạn.',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: AppTheme.textSecondary,
                  height: 1.4,
                ),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () => context.pop(),
                child: const Text('Quay lại MemByte'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

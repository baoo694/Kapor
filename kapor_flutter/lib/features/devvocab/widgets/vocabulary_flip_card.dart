import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../core/theme/app_theme.dart';
import '../data/devvocab_service.dart';

class VocabularyFlipCard extends StatefulWidget {
  final LessonVocabularyItem vocabulary;
  final bool showActions;

  const VocabularyFlipCard({
    super.key,
    required this.vocabulary,
    this.showActions = false,
  });

  @override
  State<VocabularyFlipCard> createState() => _VocabularyFlipCardState();
}

class _VocabularyFlipCardState extends State<VocabularyFlipCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _rotation;
  bool _isShowingBack = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 360),
    );
    _rotation = Tween<double>(
      begin: 0,
      end: math.pi,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _flip() {
    setState(() => _isShowingBack = !_isShowingBack);
    if (_isShowingBack) {
      _controller.forward();
    } else {
      _controller.reverse();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Lật thẻ từ vựng ${widget.vocabulary.korean}',
      child: GestureDetector(
        onTap: _flip,
        child: AnimatedBuilder(
          animation: _rotation,
          builder: (context, child) {
            final isBackVisible = _rotation.value > math.pi / 2;
            final transform = Matrix4.identity()
              ..setEntry(3, 2, 0.001)
              ..rotateY(_rotation.value);
            return Transform(
              alignment: Alignment.center,
              transform: transform,
              child: isBackVisible
                  ? Transform(
                      alignment: Alignment.center,
                      transform: Matrix4.rotationY(math.pi),
                      child: _buildBack(),
                    )
                  : _buildFront(),
            );
          },
        ),
      ),
    );
  }

  BoxDecoration _decoration() {
    return BoxDecoration(
      color: const Color(0xFF242544),
      borderRadius: BorderRadius.circular(28),
      border: Border.all(color: const Color(0xFF3A4168), width: 2),
      boxShadow: const [
        BoxShadow(
          color: Color(0x30000000),
          blurRadius: 18,
          offset: Offset(0, 10),
        ),
      ],
    );
  }

  Widget _buildFront() {
    return Container(
      width: double.infinity,
      height: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: _decoration(),
      child: Stack(
        children: [
          if (widget.showActions)
            const Align(
              alignment: Alignment.topLeft,
              child: Icon(
                Icons.volume_up_outlined,
                color: Colors.white,
                size: 30,
              ),
            ),
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  widget.vocabulary.korean,
                  textAlign: TextAlign.center,
                  style: GoogleFonts.outfit(
                    color: Colors.white,
                    fontSize: 42,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (widget.vocabulary.pronunciation.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(
                    '/${widget.vocabulary.pronunciation}/',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.jetBrainsMono(
                      color: const Color(0xFFB2B7D3),
                      fontSize: 14,
                    ),
                  ),
                ],
              ],
            ),
          ),
          if (widget.showActions)
            const Align(
              alignment: Alignment.topRight,
              child: Icon(
                Icons.star_border_rounded,
                color: Colors.white,
                size: 32,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildBack() {
    return Container(
      width: double.infinity,
      height: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: _decoration(),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.vocabulary.vietnamese.isEmpty
                  ? widget.vocabulary.english
                  : widget.vocabulary.vietnamese,
              style: GoogleFonts.outfit(
                color: AppTheme.primary,
                fontSize: 26,
                fontWeight: FontWeight.w700,
              ),
            ),
            if (widget.vocabulary.context.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text(
                widget.vocabulary.context,
                style: GoogleFonts.inter(
                  color: const Color(0xFFE6E8F1),
                  fontSize: 14,
                  height: 1.45,
                ),
              ),
            ],
            if (widget.vocabulary.codeSnippet.isNotEmpty) ...[
              const SizedBox(height: 18),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFF14152D),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  widget.vocabulary.codeSnippet,
                  style: GoogleFonts.jetBrainsMono(
                    color: AppTheme.primary,
                    fontSize: 12,
                    height: 1.45,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            Center(
              child: Text(
                'Chạm để xem lại từ tiếng Hàn',
                style: GoogleFonts.inter(
                  color: const Color(0xFF9DA3C6),
                  fontSize: 11,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

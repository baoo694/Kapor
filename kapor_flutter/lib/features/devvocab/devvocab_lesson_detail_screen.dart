import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';
import 'widgets/vocabulary_flip_card.dart';

class DevVocabLessonDetailScreen extends StatefulWidget {
  final String lessonId;
  final DevVocabLesson? initialLesson;

  const DevVocabLessonDetailScreen({
    super.key,
    required this.lessonId,
    this.initialLesson,
  });

  @override
  State<DevVocabLessonDetailScreen> createState() =>
      _DevVocabLessonDetailScreenState();
}

class _DevVocabLessonDetailScreenState
    extends State<DevVocabLessonDetailScreen> {
  final DevVocabService _devVocabService = DevVocabService();
  final PageController _pageController = PageController();
  DevVocabLesson? _lesson;
  bool _isLoading = true;
  String? _errorMessage;
  int _currentCard = 0;

  @override
  void initState() {
    super.initState();
    _lesson = widget.initialLesson;
    _loadLesson();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _loadLesson() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final lesson = await _devVocabService.getLesson(widget.lessonId);
      if (!mounted) return;
      setState(() {
        _lesson = lesson;
        _isLoading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _errorMessage = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final lesson = _lesson;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(lesson?.title ?? 'Bài học'),
      ),
      body: _buildBody(lesson),
    );
  }

  Widget _buildBody(DevVocabLesson? lesson) {
    if (lesson == null && _isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (lesson == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.wifi_off_outlined,
                color: AppTheme.textSecondary,
              ),
              const SizedBox(height: 12),
              Text(
                _errorMessage ?? 'Không thể tải bài học.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: _loadLesson,
                icon: const Icon(Icons.refresh, size: 16),
                label: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 30),
      children: [
        Text(
          lesson.titleVi,
          style: GoogleFonts.outfit(
            color: AppTheme.textPrimary,
            fontSize: 24,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          '${lesson.vocabulary.length} thuật ngữ · Chạm thẻ để lật',
          style: GoogleFonts.jetBrainsMono(
            color: AppTheme.textSecondary,
            fontSize: 11,
          ),
        ),
        const SizedBox(height: 20),
        if (lesson.vocabulary.isEmpty)
          _emptyVocabulary()
        else
          _vocabularyPreview(lesson),
        const SizedBox(height: 30),
        Text(
          'Chọn cách học',
          style: GoogleFonts.outfit(
            color: Colors.white,
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 14),
        _LearningModeTile(
          icon: Icons.style_rounded,
          title: 'Thẻ ghi nhớ',
          subtitle: 'Vuốt để đánh dấu đã biết hoặc đang học',
          color: const Color(0xFF5D72FF),
          onTap: lesson.vocabulary.isEmpty
              ? null
              : () => context.push(
                  '/devvocab-lesson/${lesson.id}/flashcards',
                  extra: lesson,
                ),
        ),
        _LearningModeTile(
          icon: Icons.auto_stories_rounded,
          title: 'Học',
          subtitle: 'Sắp ra mắt',
          color: const Color(0xFF69A5FF),
          onTap: null,
        ),
        _LearningModeTile(
          icon: Icons.fact_check_outlined,
          title: 'Kiểm tra',
          subtitle: 'Sắp ra mắt',
          color: const Color(0xFF6C78FF),
          onTap: null,
        ),
        _LearningModeTile(
          icon: Icons.compare_arrows_rounded,
          title: 'Ghép thẻ',
          subtitle: 'Sắp ra mắt',
          color: const Color(0xFF5BC9F1),
          onTap: null,
        ),
      ],
    );
  }

  Widget _vocabularyPreview(DevVocabLesson lesson) {
    return Column(
      children: [
        SizedBox(
          height: 330,
          child: PageView.builder(
            controller: _pageController,
            itemCount: lesson.vocabulary.length,
            onPageChanged: (index) => setState(() => _currentCard = index),
            itemBuilder: (context, index) => Padding(
              padding: const EdgeInsets.symmetric(horizontal: 3),
              child: VocabularyFlipCard(vocabulary: lesson.vocabulary[index]),
            ),
          ),
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(lesson.vocabulary.length, (index) {
            final isActive = index == _currentCard;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: isActive ? 18 : 8,
              height: 8,
              margin: const EdgeInsets.symmetric(horizontal: 4),
              decoration: BoxDecoration(
                color: isActive ? Colors.white : const Color(0xFF62657F),
                borderRadius: BorderRadius.circular(99),
              ),
            );
          }),
        ),
      ],
    );
  }

  Widget _emptyVocabulary() {
    return Container(
      height: 180,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      child: const Text(
        'Bài học này chưa có từ vựng.',
        style: TextStyle(color: AppTheme.textSecondary),
      ),
    );
  }
}

class _LearningModeTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Color color;
  final VoidCallback? onTap;

  const _LearningModeTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Ink(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF343D60),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(icon, color: color, size: 25),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: GoogleFonts.outfit(
                          color: Colors.white,
                          fontSize: 19,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        subtitle,
                        style: GoogleFonts.inter(
                          color: const Color(0xFFC3C7DD),
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  onTap == null
                      ? Icons.lock_outline
                      : Icons.chevron_right_rounded,
                  color: const Color(0xFFC3C7DD),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

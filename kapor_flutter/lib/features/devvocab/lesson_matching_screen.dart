import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';

class LessonMatchingScreen extends StatefulWidget {
  final String lessonId;
  final DevVocabLesson? initialLesson;

  const LessonMatchingScreen({
    super.key,
    required this.lessonId,
    this.initialLesson,
  });

  @override
  State<LessonMatchingScreen> createState() => _LessonMatchingScreenState();
}

class _LessonMatchingScreenState extends State<LessonMatchingScreen> {
  final _service = DevVocabService();
  final _random = Random();
  DevVocabLesson? _lesson;
  List<LessonVocabularyItem> _left = [];
  List<LessonVocabularyItem> _right = [];
  String? _selectedId;
  final Set<String> _matchedIds = {};
  int _mistakes = 0;
  int _elapsedSeconds = 0;
  bool _loading = true;
  bool _saving = false;
  Timer? _timer;
  String? _message;

  @override
  void initState() {
    super.initState();
    _lesson = widget.initialLesson;
    _load();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final lesson = await _service.getLesson(widget.lessonId);
      if (!mounted) return;
      _lesson = lesson;
      _left = [...lesson.vocabulary]..shuffle(_random);
      _right = [...lesson.vocabulary]..shuffle(_random);
      _timer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) setState(() => _elapsedSeconds++);
      });
      setState(() => _loading = false);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _message = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  void _chooseLeft(String id) => setState(() {
    _selectedId = _selectedId == id ? null : id;
    _message = null;
  });

  Future<void> _chooseRight(LessonVocabularyItem item) async {
    final selectedId = _selectedId;
    if (selectedId == null || _matchedIds.contains(item.id)) return;
    if (selectedId != item.id) {
      setState(() {
        _mistakes++;
        _selectedId = null;
        _message = 'Chưa đúng, thử một cặp khác nhé.';
      });
      return;
    }
    setState(() {
      _matchedIds.add(item.id);
      _selectedId = null;
      _message = 'Chính xác!';
    });
    if (_matchedIds.length == _left.length) {
      await _finish();
    }
  }

  Future<void> _finish() async {
    _timer?.cancel();
    setState(() => _saving = true);
    try {
      await _service.recordMatchingAttempt(
        lessonId: widget.lessonId,
        completedPairs: _matchedIds.length,
        mistakes: _mistakes,
        durationSeconds: _elapsedSeconds,
      );
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: const Color(0xFF202945),
          title: const Text(
            'Hoàn thành ghép thẻ!',
            style: TextStyle(color: Colors.white),
          ),
          content: Text(
            'Thời gian: ${_timeLabel(_elapsedSeconds)}\nLỗi: $_mistakes',
            style: const TextStyle(color: Color(0xFFD5D9E9)),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Xong'),
            ),
          ],
        ),
      );
      if (mounted) context.pop(true);
    } catch (error) {
      if (!mounted) return;
      setState(
        () => _message = error.toString().replaceFirst('Exception: ', ''),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String _timeLabel(int seconds) =>
      '${(seconds ~/ 60).toString().padLeft(2, '0')}:${(seconds % 60).toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(
        backgroundColor: AppTheme.background,
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (_lesson == null || _left.isEmpty) {
      return Scaffold(
        backgroundColor: AppTheme.background,
        appBar: AppBar(title: const Text('Ghép thẻ')),
        body: Center(
          child: Text(
            _message ?? 'Bài học chưa có thẻ.',
            style: const TextStyle(color: AppTheme.textSecondary),
          ),
        ),
      );
    }
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Ghép thẻ'),
        backgroundColor: const Color(0xFF102126),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                _Metric(
                  icon: Icons.timer_outlined,
                  label: _timeLabel(_elapsedSeconds),
                ),
                const SizedBox(width: 10),
                _Metric(icon: Icons.close_rounded, label: '$_mistakes lỗi'),
                const Spacer(),
                Text(
                  '${_matchedIds.length}/${_left.length}',
                  style: GoogleFonts.jetBrainsMono(
                    color: AppTheme.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text(
              _message ?? 'Chọn từ tiếng Hàn, sau đó chọn nghĩa đúng.',
              style: GoogleFonts.inter(
                color: AppTheme.textSecondary,
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 14),
            Expanded(
              child: Row(
                children: [
                  Expanded(
                    child: _WordColumn(
                      items: _left,
                      matchedIds: _matchedIds,
                      selectedId: _selectedId,
                      labelFor: (word) => word.korean,
                      onTap: (word) => _chooseLeft(word.id),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _WordColumn(
                      items: _right,
                      matchedIds: _matchedIds,
                      selectedId: null,
                      labelFor: (word) => word.vietnamese,
                      onTap: _chooseRight,
                    ),
                  ),
                ],
              ),
            ),
            if (_saving)
              const Padding(
                padding: EdgeInsets.only(top: 12),
                child: LinearProgressIndicator(),
              ),
          ],
        ),
      ),
    );
  }
}

class _WordColumn extends StatelessWidget {
  final List<LessonVocabularyItem> items;
  final Set<String> matchedIds;
  final String? selectedId;
  final String Function(LessonVocabularyItem) labelFor;
  final ValueChanged<LessonVocabularyItem> onTap;
  const _WordColumn({
    required this.items,
    required this.matchedIds,
    required this.selectedId,
    required this.labelFor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) => ListView.separated(
    itemCount: items.length,
    separatorBuilder: (context, index) => const SizedBox(height: 8),
    itemBuilder: (context, index) {
      final item = items[index];
      final matched = matchedIds.contains(item.id);
      final selected = selectedId == item.id;
      return Semantics(
        button: !matched,
        selected: selected,
        label: labelFor(item),
        child: InkWell(
          onTap: matched ? null : () => onTap(item),
          borderRadius: BorderRadius.circular(14),
          child: Ink(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
            decoration: BoxDecoration(
              color: matched
                  ? const Color(0xFF173D38)
                  : selected
                  ? const Color(0xFF314879)
                  : AppTheme.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: matched
                    ? const Color(0xFF55D8AD)
                    : selected
                    ? AppTheme.primary
                    : Colors.white.withValues(alpha: .08),
              ),
            ),
            child: Text(
              labelFor(item),
              textAlign: TextAlign.center,
              style: GoogleFonts.outfit(
                color: matched ? const Color(0xFF8AEDD0) : Colors.white,
                fontWeight: FontWeight.w600,
                fontSize: 15,
              ),
            ),
          ),
        ),
      );
    },
  );
}

class _Metric extends StatelessWidget {
  final IconData icon;
  final String label;
  const _Metric({required this.icon, required this.label});
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
    decoration: BoxDecoration(
      color: AppTheme.surface,
      borderRadius: BorderRadius.circular(99),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 16, color: AppTheme.textSecondary),
        const SizedBox(width: 5),
        Text(
          label,
          style: GoogleFonts.jetBrainsMono(color: Colors.white, fontSize: 11),
        ),
      ],
    ),
  );
}

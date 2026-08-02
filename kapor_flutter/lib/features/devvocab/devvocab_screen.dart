import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_theme.dart';
import 'data/devvocab_service.dart';

class DevVocabScreen extends StatefulWidget {
  const DevVocabScreen({super.key});

  @override
  State<DevVocabScreen> createState() => _DevVocabScreenState();
}

class _DevVocabScreenState extends State<DevVocabScreen> {
  String selectedDomain = 'all';
  final List<String> domains = [
    "all",
    "frontend",
    "backend",
    "devops",
    "agile",
  ];
  final DevVocabService _devVocabService = DevVocabService();
  List<DevVocabTopic> _topics = const [];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadTopics();
  }

  Future<void> _loadTopics() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final results = await Future.wait(
        domains
            .where((domain) => domain != 'all')
            .map(_devVocabService.getTopics),
      );
      if (!mounted) return;

      setState(() {
        _topics = results.expand((topics) => topics).toList();
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

  Color _getDomainColor(String d) {
    if (d == 'frontend') return AppTheme.primary;
    if (d == 'devops') return Colors.orange;
    if (d == 'agile') return Colors.greenAccent;
    if (d == 'backend') return Colors.purpleAccent;
    return AppTheme.textSecondary;
  }

  @override
  Widget build(BuildContext context) {
    final filteredNodes = selectedDomain == 'all'
        ? _topics
        : _topics.where((topic) => topic.domain == selectedDomain).toList();

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Text(
              'DevVocab',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
            ),
            Text(
              'Từ vựng IT chuyên ngành tiếng Hàn',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                fontSize: 10,
                color: AppTheme.textSecondary,
              ),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          _buildFilterTabs(),
          Expanded(child: _buildTopicList(filteredNodes)),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showSummarizerSheet(context),
        backgroundColor: AppTheme.primary,
        child: const Icon(LucideIcons.plus, color: Colors.black),
      ),
    );
  }

  Widget _buildTopicList(List<DevVocabTopic> topics) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                LucideIcons.wifiOff,
                color: AppTheme.textSecondary,
                size: 32,
              ),
              const SizedBox(height: 12),
              Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: _loadTopics,
                icon: const Icon(LucideIcons.refreshCw, size: 16),
                label: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    if (topics.isEmpty) {
      return RefreshIndicator(
        onRefresh: _loadTopics,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(24),
          children: const [
            SizedBox(height: 120),
            Icon(LucideIcons.bookOpen, color: AppTheme.textSecondary, size: 32),
            SizedBox(height: 12),
            Text(
              'Chưa có topic nào trong domain này.',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppTheme.textSecondary),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadTopics,
      child: ListView.separated(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        itemCount: topics.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final topic = topics[index];
          return GestureDetector(
            onTap: topic.isLocked
                ? null
                : () =>
                      context.push('/devvocab-topic/${topic.id}', extra: topic),
            child: _buildSkillNodeCard(topic),
          );
        },
      ),
    );
  }

  Widget _buildFilterTabs() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: domains.map((d) {
          final isSelected = selectedDomain == d;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: GestureDetector(
              onTap: () => setState(() => selectedDomain = d),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: isSelected ? AppTheme.primary : AppTheme.surface,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: isSelected
                        ? AppTheme.primary
                        : Colors.white.withOpacity(0.1),
                  ),
                ),
                child: Text(
                  d == 'all' ? 'Tất cả' : d[0].toUpperCase() + d.substring(1),
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: isSelected ? Colors.black : AppTheme.textSecondary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildSkillNodeCard(DevVocabTopic topic) {
    final completionPercent = topic.completionPercent.clamp(0, 100);
    final col = completionPercent == 100
        ? Colors.greenAccent
        : AppTheme.primary;
    final domainCol = _getDomainColor(topic.domain);

    return Opacity(
      opacity: topic.isLocked ? 0.6 : 1.0,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: topic.isLocked ? AppTheme.background : AppTheme.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: completionPercent == 100
                ? Colors.greenAccent.withOpacity(0.4)
                : topic.isLocked
                ? Colors.white.withOpacity(0.05)
                : AppTheme.primary.withOpacity(0.3),
          ),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: topic.isLocked
                    ? Colors.white.withOpacity(0.05)
                    : col.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                topic.isLocked
                    ? LucideIcons.lock
                    : completionPercent == 100
                    ? LucideIcons.checkCircle
                    : LucideIcons.bookOpen,
                color: topic.isLocked ? AppTheme.textSecondary : col,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    topic.title,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      color: topic.isLocked
                          ? AppTheme.textSecondary
                          : AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    topic.titleVi,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontSize: 11,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  if (!topic.isLocked) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(2),
                            child: LinearProgressIndicator(
                              value: completionPercent / 100,
                              backgroundColor: Colors.white.withOpacity(0.1),
                              valueColor: AlwaysStoppedAnimation<Color>(col),
                              minHeight: 4,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '${completionPercent.round()}%',
                          style: Theme.of(context).textTheme.labelLarge
                              ?.copyWith(fontSize: 10, color: col),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: domainCol.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: domainCol.withOpacity(0.3)),
                  ),
                  child: Text(
                    topic.domain,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontSize: 9,
                      color: domainCol,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  '${topic.completedLessons} / ${topic.totalLessons} bài',
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showSummarizerSheet(BuildContext context) async {
    final saved = await showModalBottomSheet<SummarizerSavedDeck>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => const _SummarizerSheet(),
    );
    if (!context.mounted || saved == null) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã lưu ${saved.savedCards} thẻ vào MemByte.'),
        action: SnackBarAction(
          label: 'MỞ MEMBYTE',
          onPressed: () => context.go('/membyte'),
        ),
      ),
    );
  }
}

class _SummarizerSheet extends StatefulWidget {
  const _SummarizerSheet();

  @override
  State<_SummarizerSheet> createState() => _SummarizerSheetState();
}

class _SummarizerSheetState extends State<_SummarizerSheet> {
  final _service = DevVocabService();
  final _inputController = TextEditingController();
  final _titleController = TextEditingController();
  final Set<int> _selectedIndexes = {};
  SummarizerPreview? _preview;
  String? _error;
  bool _isGenerating = false;
  bool _isSaving = false;

  @override
  void dispose() {
    _inputController.dispose();
    _titleController.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    final input = _inputController.text.trim();
    if (input.isEmpty) {
      setState(() => _error = 'Hãy dán URL hoặc nội dung bài viết tiếng Hàn.');
      return;
    }
    setState(() {
      _isGenerating = true;
      _error = null;
    });
    try {
      final preview = await _service.generateFlashcards(input);
      if (!mounted) return;
      setState(() {
        _preview = preview;
        _titleController.text = 'AI Summary · ${preview.title}';
        _selectedIndexes
          ..clear()
          ..addAll(List<int>.generate(preview.cards.length, (index) => index));
      });
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _save() async {
    final preview = _preview;
    if (preview == null || _selectedIndexes.isEmpty) {
      setState(() => _error = 'Hãy chọn ít nhất một flashcard để lưu.');
      return;
    }
    setState(() {
      _isSaving = true;
      _error = null;
    });
    try {
      final cards = _selectedIndexes
          .map((index) => preview.cards[index])
          .toList();
      final saved = await _service.saveSummarizerDeck(
        preview: preview,
        title: _titleController.text.trim(),
        cards: cards,
      );
      if (mounted) {
        Navigator.pop(context, saved);
      }
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final preview = _preview;
    final busy = _isGenerating || _isSaving;
    return SafeArea(
      child: SizedBox(
        height: MediaQuery.of(context).size.height * .84,
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            20,
            20,
            20,
            MediaQuery.of(context).viewInsets.bottom + 16,
          ),
          child: Column(
            children: [
              Row(
                children: [
                  if (preview != null)
                    IconButton(
                      icon: const Icon(LucideIcons.arrowLeft, size: 20),
                      onPressed: busy
                          ? null
                          : () => setState(() => _preview = null),
                    ),
                  const Text(
                    'SmartSummarizer',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(width: 8),
                  _AiBadge(),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(LucideIcons.x, size: 22),
                    onPressed: busy ? null : () => Navigator.pop(context),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Expanded(
                child: preview == null
                    ? _sourceForm(busy)
                    : _previewList(preview, busy),
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(
                  _error!,
                  style: const TextStyle(color: Colors.redAccent),
                  textAlign: TextAlign.center,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _sourceForm(bool busy) => ListView(
    children: [
      const Text(
        'Tạo thẻ từ bài viết IT tiếng Hàn',
        style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
      const SizedBox(height: 8),
      const Text(
        'Dán URL công khai hoặc nội dung văn bản. Bạn sẽ xem và chọn thẻ trước khi lưu.',
        style: TextStyle(color: AppTheme.textSecondary),
      ),
      const SizedBox(height: 18),
      TextField(
        controller: _inputController,
        enabled: !busy,
        minLines: 6,
        maxLines: 10,
        decoration: _inputDecoration(
          'Dán URL bài viết IT tiếng Hàn hoặc nội dung văn bản...',
        ),
      ),
      const SizedBox(height: 16),
      SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          onPressed: busy ? null : _generate,
          icon: busy
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.black,
                  ),
                )
              : const Icon(LucideIcons.sparkles, color: Colors.black, size: 18),
          label: Text(
            busy ? 'Đang phân tích…' : 'Tạo Flashcards',
            style: const TextStyle(
              color: Colors.black,
              fontWeight: FontWeight.bold,
            ),
          ),
          style: _primaryButtonStyle(),
        ),
      ),
    ],
  );

  Widget _previewList(SummarizerPreview preview, bool busy) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        '${preview.cards.length} thẻ được đề xuất',
        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
      const SizedBox(height: 3),
      Text(
        'Chọn các thẻ muốn lưu vào MemByte.',
        style: const TextStyle(color: AppTheme.textSecondary),
      ),
      const SizedBox(height: 12),
      TextField(
        controller: _titleController,
        enabled: !busy,
        decoration: _inputDecoration('Tên deck'),
      ),
      const SizedBox(height: 8),
      Expanded(
        child: ListView.separated(
          itemCount: preview.cards.length,
          separatorBuilder: (context, index) => const SizedBox(height: 8),
          itemBuilder: (context, index) {
            final card = preview.cards[index];
            final selected = _selectedIndexes.contains(index);
            return CheckboxListTile(
              value: selected,
              onChanged: busy
                  ? null
                  : (value) => setState(
                      () => value == true
                          ? _selectedIndexes.add(index)
                          : _selectedIndexes.remove(index),
                    ),
              activeColor: AppTheme.primary,
              tileColor: AppTheme.background,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
                side: BorderSide(color: Colors.white.withValues(alpha: .08)),
              ),
              title: Text(
                card.korean,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              subtitle: Text(
                '${card.vietnamese}${card.pronunciation.isEmpty ? '' : ' · ${card.pronunciation}'}',
                style: const TextStyle(color: AppTheme.textSecondary),
              ),
              controlAffinity: ListTileControlAffinity.leading,
            );
          },
        ),
      ),
      const SizedBox(height: 12),
      SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          onPressed: busy ? null : _save,
          icon: busy
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.black,
                  ),
                )
              : const Icon(LucideIcons.save, color: Colors.black, size: 18),
          label: Text(
            busy
                ? 'Đang lưu…'
                : 'Lưu ${_selectedIndexes.length} thẻ vào MemByte',
            style: const TextStyle(
              color: Colors.black,
              fontWeight: FontWeight.bold,
            ),
          ),
          style: _primaryButtonStyle(),
        ),
      ),
    ],
  );

  InputDecoration _inputDecoration(String hint) => InputDecoration(
    hintText: hint,
    hintStyle: const TextStyle(color: AppTheme.textSecondary),
    filled: true,
    fillColor: AppTheme.background,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: BorderSide(color: Colors.white.withValues(alpha: .1)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppTheme.primary),
    ),
  );

  ButtonStyle _primaryButtonStyle() => ElevatedButton.styleFrom(
    backgroundColor: AppTheme.primary,
    padding: const EdgeInsets.symmetric(vertical: 14),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
  );
}

class _AiBadge extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
    decoration: BoxDecoration(
      color: AppTheme.primary.withValues(alpha: .2),
      borderRadius: BorderRadius.circular(6),
      border: Border.all(color: AppTheme.primary.withValues(alpha: .4)),
    ),
    child: const Text(
      'AI',
      style: TextStyle(
        color: AppTheme.primary,
        fontSize: 10,
        fontWeight: FontWeight.bold,
      ),
    ),
  );
}

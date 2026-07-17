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

  void _showSummarizerSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
          left: 16,
          right: 16,
          top: 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Text(
                      'SmartSummarizer',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(
                          color: AppTheme.primary.withOpacity(0.4),
                        ),
                      ),
                      child: const Text(
                        'AI',
                        style: TextStyle(
                          color: AppTheme.primary,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(LucideIcons.x, size: 20),
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
            const SizedBox(height: 16),
            TextField(
              maxLines: 4,
              decoration: InputDecoration(
                hintText:
                    'Dán URL bài viết IT tiếng Hàn hoặc nội dung văn bản...',
                hintStyle: const TextStyle(
                  color: AppTheme.textSecondary,
                  fontSize: 14,
                ),
                filled: true,
                fillColor: AppTheme.background,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide(color: Colors.white.withOpacity(0.1)),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: AppTheme.primary),
                ),
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                },
                icon: const Icon(
                  LucideIcons.sparkles,
                  color: Colors.black,
                  size: 16,
                ),
                label: const Text(
                  'Tạo Flashcards',
                  style: TextStyle(
                    color: Colors.black,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

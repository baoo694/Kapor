import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_theme.dart';

class SkillNode {
  final String id;
  final String title;
  final String titleVi;
  final String domain;
  final bool locked;
  final int pct;
  final int lessons;

  SkillNode({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.domain,
    required this.locked,
    required this.pct,
    required this.lessons,
  });
}

final List<SkillNode> mockSkillNodes = [
  SkillNode(id: "html", title: "HTML & DOM 용어", titleVi: "Thuật ngữ HTML & DOM", domain: "frontend", locked: false, pct: 100, lessons: 5),
  SkillNode(id: "css", title: "CSS Grid & Flexbox 용어", titleVi: "Thuật ngữ CSS Grid & Flexbox", domain: "frontend", locked: false, pct: 65, lessons: 5),
  SkillNode(id: "deploy", title: "배포 & CI/CD 용어", titleVi: "Thuật ngữ Deployment & CI/CD", domain: "devops", locked: false, pct: 30, lessons: 6),
  SkillNode(id: "api", title: "REST API 설계 용어", titleVi: "Thuật ngữ thiết kế REST API", domain: "backend", locked: true, pct: 0, lessons: 4),
  SkillNode(id: "docker", title: "Docker & 컨테이너", titleVi: "Docker & Container", domain: "devops", locked: true, pct: 0, lessons: 5),
];

class DevVocabScreen extends StatefulWidget {
  const DevVocabScreen({super.key});

  @override
  State<DevVocabScreen> createState() => _DevVocabScreenState();
}

class _DevVocabScreenState extends State<DevVocabScreen> {
  String selectedDomain = 'all';
  final List<String> domains = ["all", "frontend", "backend", "devops", "agile"];

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
        ? mockSkillNodes
        : mockSkillNodes.where((n) => n.domain == selectedDomain).toList();

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Text('DevVocab', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20)),
            Text(
              'Từ vựng IT chuyên ngành tiếng Hàn',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(fontSize: 10, color: AppTheme.textSecondary),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          _buildFilterTabs(),
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: filteredNodes.length,
              separatorBuilder: (context, index) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final node = filteredNodes[index];
                return GestureDetector(
                  onTap: () {
                    if (!node.locked) {
                      context.push('/devvocab-lesson/${node.id}');
                    }
                  },
                  child: _buildSkillNodeCard(node),
                );
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showSummarizerSheet(context),
        backgroundColor: AppTheme.primary,
        child: const Icon(LucideIcons.plus, color: Colors.black),
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
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                decoration: BoxDecoration(
                  color: isSelected ? AppTheme.primary : AppTheme.surface,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: isSelected ? AppTheme.primary : Colors.white.withOpacity(0.1),
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

  Widget _buildSkillNodeCard(SkillNode node) {
    final col = node.pct == 100 ? Colors.greenAccent : AppTheme.primary;
    final domainCol = _getDomainColor(node.domain);

    return Opacity(
      opacity: node.locked ? 0.6 : 1.0,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: node.locked ? AppTheme.background : AppTheme.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: node.pct == 100
                ? Colors.greenAccent.withOpacity(0.4)
                : node.locked
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
                color: node.locked ? Colors.white.withOpacity(0.05) : col.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                node.locked
                    ? LucideIcons.lock
                    : node.pct == 100
                        ? LucideIcons.checkCircle
                        : LucideIcons.bookOpen,
                color: node.locked ? AppTheme.textSecondary : col,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    node.title,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      color: node.locked ? AppTheme.textSecondary : AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    node.titleVi,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          fontSize: 11,
                          color: AppTheme.textSecondary,
                        ),
                  ),
                  if (!node.locked) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(2),
                            child: LinearProgressIndicator(
                              value: node.pct / 100,
                              backgroundColor: Colors.white.withOpacity(0.1),
                              valueColor: AlwaysStoppedAnimation<Color>(col),
                              minHeight: 4,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '${node.pct}%',
                          style: Theme.of(context).textTheme.labelLarge?.copyWith(
                                fontSize: 10,
                                color: col,
                              ),
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
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: domainCol.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: domainCol.withOpacity(0.3)),
                  ),
                  child: Text(
                    node.domain,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          fontSize: 9,
                          color: domainCol,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  '${node.lessons} bài',
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
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: AppTheme.primary.withOpacity(0.4)),
                      ),
                      child: const Text(
                        'AI',
                        style: TextStyle(color: AppTheme.primary, fontSize: 10, fontWeight: FontWeight.bold),
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
                hintText: 'Dán URL bài viết IT tiếng Hàn hoặc nội dung văn bản...',
                hintStyle: const TextStyle(color: AppTheme.textSecondary, fontSize: 14),
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
                icon: const Icon(LucideIcons.sparkles, color: Colors.black, size: 16),
                label: const Text('Tạo Flashcards', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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

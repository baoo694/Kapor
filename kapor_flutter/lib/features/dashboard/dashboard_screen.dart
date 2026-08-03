import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../auth/providers/auth_provider.dart';
import 'data/dashboard_service.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final DashboardService _dashboardService = DashboardService();
  DashboardPeriod _period = DashboardPeriod.weekly;
  DashboardData? _dashboard;
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadDashboard();
  }

  Future<void> _loadDashboard() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final dashboard = await _dashboardService.getDashboard(_period);
      if (!mounted) return;
      setState(() {
        _dashboard = dashboard;
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

  void _selectPeriod(DashboardPeriod period) {
    if (_period == period && _dashboard != null) return;
    setState(() => _period = period);
    _loadDashboard();
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;
    final displayName = user?['displayName']?.toString().trim();

    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 60,
        centerTitle: false,
        titleSpacing: 20, // Tương đương padding left 20px
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Xin chào 👋',
              style: TextStyle(
                fontSize: 11,
                color: AppTheme.textSecondary,
                fontFamily: 'JetBrains Mono',
              ),
            ),
            const SizedBox(height: 2),
            Text(
              displayName?.isNotEmpty == true ? displayName! : 'Đang tải…',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w800,
                fontFamily: 'Outfit',
                color: Colors.white,
              ),
            ),
          ],
        ),
        actions: [
          Container(
            margin: const EdgeInsets.only(right: 20),
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: const Color(
                0xFF00BFA5,
              ).withValues(alpha: 0.2), // Tương đương TEAL20
              border: Border.all(
                color: const Color(0xFF00BFA5).withValues(alpha: 0.44),
              ),
            ),
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(
                LucideIcons.user,
                color: Color(0xFF00BFA5),
                size: 15,
              ),
              onPressed: () {},
            ),
          ),
        ],
      ),
      body: _buildBody(context),
    );
  }

  Widget _buildBody(BuildContext context) {
    if (_isLoading && _dashboard == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_errorMessage != null && _dashboard == null) {
      return _DashboardError(message: _errorMessage!, onRetry: _loadDashboard);
    }
    final dashboard = _dashboard;
    if (dashboard == null) {
      return _DashboardError(
        message: 'Không thể tải dashboard.',
        onRetry: _loadDashboard,
      );
    }

    return RefreshIndicator(
      onRefresh: _loadDashboard,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildStreakCard(dashboard.streak),
            const SizedBox(height: 16),
            _buildDailyGoalCard(dashboard.dailyGoal),
            const SizedBox(height: 16),
            _buildProgressCard(dashboard.progress),
            const SizedBox(height: 16),
            if (dashboard.recommendation != null)
              _buildRecommendationCard(context, dashboard.recommendation!),
            const SizedBox(height: 24),
            Text(
              'KHÁM PHÁ',
              style: TextStyle(
                fontSize: 10,
                color: Colors.white.withValues(alpha: 0.40),
                fontFamily: 'JetBrains Mono',
                letterSpacing: 1,
              ),
            ),
            const SizedBox(height: 10),
            _buildQuickNavGrid(context),
          ],
        ),
      ),
    );
  }

  Widget _buildStreakCard(DashboardStreak streak) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
      child: Row(
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: AppTheme.secondary.withValues(alpha: 0.22),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Center(
                  child: Icon(
                    LucideIcons.flame,
                    color: AppTheme.secondary,
                    size: 22,
                  ),
                ),
              ),
              Positioned(
                right: -4,
                top: -4,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: streak.isActiveToday
                        ? const Color(0xFF00BFA5)
                        : AppTheme.textSecondary,
                    shape: BoxShape.circle,
                  ),
                  child: const Center(
                    child: Icon(Icons.check, color: Colors.black, size: 9),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text.rich(
                  TextSpan(
                    children: [
                      TextSpan(
                        text: streak.currentStreak.toString(),
                        style: TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w800,
                          fontFamily: 'Outfit',
                          color: AppTheme.secondary,
                        ),
                      ),
                      const WidgetSpan(child: SizedBox(width: 6)),
                      TextSpan(
                        text: 'ngày liên tiếp',
                        style: TextStyle(
                          fontSize: 13,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Kỷ lục: ${streak.longestStreak} ngày',
                  style: TextStyle(
                    fontSize: 10,
                    color: Colors.white.withValues(alpha: 0.45),
                    fontFamily: 'JetBrains Mono',
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFF052E26),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: const Color(0xFF00BFA5).withValues(alpha: 0.3),
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  streak.isActiveToday ? 'Hôm nay' : 'Chưa học',
                  style: TextStyle(
                    color: const Color(0xFF00BFA5),
                    fontWeight: FontWeight.bold,
                    fontSize: 10,
                  ),
                ),
                const SizedBox(width: 4),
                Icon(
                  streak.isActiveToday ? Icons.check : Icons.schedule,
                  color: const Color(0xFF00BFA5),
                  size: 10,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendationCard(
    BuildContext context,
    DashboardRecommendation recommendation,
  ) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: const Color(0xFF00BFA5).withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: const Color(0xFF00BFA5).withValues(alpha: 0.30),
        ),
      ),
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: const Color(0xFF00BFA5).withValues(alpha: 0.28),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Center(
              child: Text(
                recommendation.icon,
                style: const TextStyle(fontSize: 17),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'TIẾP THEO',
                  style: TextStyle(
                    fontSize: 9,
                    color: Color(0xFF00BFA5),
                    fontFamily: 'JetBrains Mono',
                    letterSpacing: 1,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  recommendation.title,
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Colors.white.withValues(alpha: 0.92),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  recommendation.subtitle,
                  style: TextStyle(
                    fontSize: 11,
                    color: Colors.white.withValues(alpha: 0.50),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          ElevatedButton(
            onPressed: () => _openRecommendation(context, recommendation),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF00BFA5),
              foregroundColor: Colors.black,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
              minimumSize: const Size(0, 30),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
              elevation: 0,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: const [
                Text(
                  'Học',
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
                SizedBox(width: 4),
                Icon(LucideIcons.arrowRight, size: 12),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _openRecommendation(
    BuildContext context,
    DashboardRecommendation recommendation,
  ) {
    final route = recommendation.targetScreen;
    if (route.isEmpty || !route.startsWith('/')) return;

    final destination = Uri.parse(route).replace(
      queryParameters: {
        ...Uri.parse(route).queryParameters,
        'from': 'dashboard',
      },
    );
    // A recommendation opens as a detail flow. Keep Dashboard in the stack so
    // the review screen's back button returns the user to where they started.
    context.push(destination.toString());
  }

  Widget _buildDailyGoalCard(DashboardDailyGoal goal) {
    final progress = (goal.percentComplete.clamp(0, 100) / 100).toDouble();
    final remaining = (goal.targetMinutes - goal.studiedMinutes).clamp(
      0,
      goal.targetMinutes,
    );
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: goal.completed
              ? const Color(0xFF00BFA5).withValues(alpha: 0.42)
              : Colors.white.withValues(alpha: 0.05),
        ),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: const Color(0xFF00BFA5).withValues(alpha: 0.16),
                  borderRadius: BorderRadius.circular(11),
                ),
                child: const Icon(
                  LucideIcons.target,
                  color: Color(0xFF2DD4BF),
                  size: 19,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Mục tiêu hôm nay',
                      style: TextStyle(
                        color: Colors.white,
                        fontFamily: 'Outfit',
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      goal.completed
                          ? 'Bạn đã hoàn thành mục tiêu hôm nay!'
                          : 'Còn $remaining phút để hoàn thành mục tiêu',
                      style: const TextStyle(
                        color: AppTheme.textSecondary,
                        fontSize: 11,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                '${goal.studiedMinutes}/${goal.targetMinutes}m',
                style: const TextStyle(
                  color: Color(0xFF2DD4BF),
                  fontFamily: 'JetBrains Mono',
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: const AlwaysStoppedAnimation(Color(0xFF00D4E7)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressCard(DashboardProgress progress) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Tiến độ học tập',
                style: TextStyle(
                  fontFamily: 'Outfit',
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                  color: Colors.white.withValues(alpha: 0.85),
                ),
              ),
              Row(
                children: [
                  _buildPeriodButton(
                    'Tuần',
                    _period == DashboardPeriod.weekly,
                    () => _selectPeriod(DashboardPeriod.weekly),
                  ),
                  const SizedBox(width: 4),
                  _buildPeriodButton(
                    'Tháng',
                    _period == DashboardPeriod.monthly,
                    () => _selectPeriod(DashboardPeriod.monthly),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (_isLoading)
            const SizedBox(
              height: 150,
              child: Center(child: CircularProgressIndicator()),
            )
          else if (!progress.hasData)
            SizedBox(
              height: 150,
              child: Center(
                child: Text(
                  'Chưa có dữ liệu tiến độ trong ${_period == DashboardPeriod.weekly ? 'tuần' : 'tháng'} này.',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: AppTheme.textSecondary,
                    fontSize: 12,
                  ),
                ),
              ),
            )
          else
            SizedBox(
              height: 150,
              width: double.infinity,
              child: Padding(
                padding: const EdgeInsets.only(top: 16.0, bottom: 8.0),
                child: BarChart(
                  BarChartData(
                    alignment: BarChartAlignment.spaceAround,
                    maxY: 100,
                    barTouchData: BarTouchData(enabled: false),
                    titlesData: FlTitlesData(
                      show: true,
                      bottomTitles: AxisTitles(
                        sideTitles: SideTitles(
                          showTitles: true,
                          getTitlesWidget: (value, meta) {
                            const style = TextStyle(
                              color: AppTheme.textSecondary,
                              fontSize: 10,
                              fontFamily: 'JetBrains Mono',
                            );
                            String text;
                            switch (value.toInt()) {
                              case 0:
                                text = 'Nói';
                                break;
                              case 1:
                                text = 'Từ vựng';
                                break;
                              case 2:
                                text = 'Nghe';
                                break;
                              case 3:
                                text = 'Roleplay';
                                break;
                              default:
                                text = '';
                                break;
                            }
                            return SideTitleWidget(
                              meta: meta,
                              space: 6,
                              child: Text(text, style: style),
                            );
                          },
                          reservedSize: 22,
                        ),
                      ),
                      leftTitles: const AxisTitles(
                        sideTitles: SideTitles(showTitles: false),
                      ),
                      topTitles: const AxisTitles(
                        sideTitles: SideTitles(showTitles: false),
                      ),
                      rightTitles: const AxisTitles(
                        sideTitles: SideTitles(showTitles: false),
                      ),
                    ),
                    gridData: FlGridData(
                      show: true,
                      drawVerticalLine: false,
                      horizontalInterval: 25,
                      getDrawingHorizontalLine: (value) => FlLine(
                        color: Colors.white.withValues(alpha: 0.05),
                        strokeWidth: 1,
                      ),
                    ),
                    borderData: FlBorderData(show: false),
                    barGroups: [
                      BarChartGroupData(
                        x: 0,
                        barRods: [
                          BarChartRodData(
                            toY: progress.speaking.toDouble(),
                            color: const Color(0xFFa78bfa),
                            width: 14,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ],
                      ),
                      BarChartGroupData(
                        x: 1,
                        barRods: [
                          BarChartRodData(
                            toY: progress.vocabulary.toDouble(),
                            color: const Color(0xFF00BFA5),
                            width: 14,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ],
                      ),
                      BarChartGroupData(
                        x: 2,
                        barRods: [
                          BarChartRodData(
                            toY: progress.listening.toDouble(),
                            color: const Color(0xFFfb923c),
                            width: 14,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ],
                      ),
                      BarChartGroupData(
                        x: 3,
                        barRods: [
                          BarChartRodData(
                            toY: progress.roleplay.toDouble(),
                            color: const Color(0xFF34d399),
                            width: 14,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ],
                      ),
                    ],
                  ),
                  duration: const Duration(milliseconds: 150),
                  curve: Curves.linear,
                ),
              ),
            ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildSkillStat(
                'Nói',
                progress.speaking,
                const Color(0xFFa78bfa),
                progress.hasData,
              ),
              _buildSkillStat(
                'Từ vựng',
                progress.vocabulary,
                const Color(0xFF00BFA5),
                progress.hasData,
              ),
              _buildSkillStat(
                'Nghe',
                progress.listening,
                const Color(0xFFfb923c),
                progress.hasData,
              ),
              _buildSkillStat(
                'Roleplay',
                progress.roleplay,
                const Color(0xFF34d399),
                progress.hasData,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPeriodButton(String text, bool isActive, VoidCallback onTap) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(6),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: isActive ? const Color(0xFF00BFA5) : const Color(0xFF2C2C2E),
            borderRadius: BorderRadius.circular(6),
          ),
          child: Text(
            text,
            style: TextStyle(
              fontFamily: 'JetBrains Mono',
              fontSize: 10,
              color: isActive ? Colors.black : AppTheme.textSecondary,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSkillStat(String label, int value, Color color, bool hasData) {
    return Expanded(
      child: Column(
        children: [
          Text(
            hasData ? value.toString() : '—',
            style: TextStyle(
              fontFamily: 'Outfit',
              fontWeight: FontWeight.w700,
              fontSize: 15,
              color: color,
            ),
          ),
          Text(
            label,
            style: TextStyle(
              fontFamily: 'JetBrains Mono',
              fontSize: 9,
              color: AppTheme.textSecondary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildQuickNavGrid(BuildContext context) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 10,
      mainAxisSpacing: 10,
      childAspectRatio: 1.5,
      children: [
        _buildNavCard(
          context,
          'Video Lab',
          'Phim kỹ thuật',
          LucideIcons.play,
          const Color(0xFFfb923c),
          '/video',
        ),
        _buildNavCard(
          context,
          'TechTalk AI',
          'Roleplay IT',
          LucideIcons.messageSquare,
          const Color(0xFFa78bfa),
          '/techtalk-select',
        ),
        _buildNavCard(
          context,
          'Phát âm',
          'Luyện giọng',
          LucideIcons.mic,
          const Color(0xFF34d399),
          '/pronunciation-list',
        ),
        _buildNavCard(
          context,
          'Honorifics',
          'Ngữ pháp tôn kính',
          LucideIcons.target,
          const Color(0xFFfbbf24),
          '/honorifics',
        ),
      ],
    );
  }

  Widget _buildNavCard(
    BuildContext context,
    String title,
    String subTitle,
    IconData icon,
    Color color,
    String route,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withValues(alpha: 0.28)),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            context.push(route);
          },
          borderRadius: BorderRadius.circular(14),
          child: Padding(
            padding: const EdgeInsets.all(14.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(9),
                  ),
                  child: Center(child: Icon(icon, color: color, size: 15)),
                ),
                const SizedBox(height: 8),
                Text(
                  title,
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Colors.white.withValues(alpha: 0.90),
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  subTitle,
                  style: TextStyle(
                    fontFamily: 'JetBrains Mono',
                    fontSize: 10,
                    color: Colors.white.withValues(alpha: 0.45),
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DashboardError extends StatelessWidget {
  const _DashboardError({required this.message, required this.onRetry});

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(LucideIcons.wifiOff, color: AppTheme.textSecondary),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(LucideIcons.refreshCw, size: 16),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }
}

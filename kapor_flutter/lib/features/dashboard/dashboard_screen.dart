import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_theme.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
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
              'Nguyễn Văn A',
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
              color: const Color(0xFF00BFA5).withOpacity(0.2), // Tương đương TEAL20
              border: Border.all(color: const Color(0xFF00BFA5).withOpacity(0.44)),
            ),
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(LucideIcons.user, color: Color(0xFF00BFA5), size: 15),
              onPressed: () {},
            ),
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildStreakCard(),
            const SizedBox(height: 16),
            _buildProgressCard(),
            const SizedBox(height: 16),
            _buildRecommendationCard(),
            const SizedBox(height: 24),
            Text(
              'KHÁM PHÁ',
              style: TextStyle(
                fontSize: 10,
                color: Colors.white.withOpacity(0.40),
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

  Widget _buildStreakCard() {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
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
                  color: AppTheme.secondary.withOpacity(0.22),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Center(
                  child: Icon(LucideIcons.flame, color: AppTheme.secondary, size: 22),
                ),
              ),
              Positioned(
                right: -4,
                top: -4,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: const BoxDecoration(
                    color: Color(0xFF00BFA5),
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
                        text: '15',
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
                  'Kỷ lục: 30 ngày',
                  style: TextStyle(
                    fontSize: 10,
                    color: Colors.white.withOpacity(0.45),
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
              border: Border.all(color: const Color(0xFF00BFA5).withOpacity(0.3)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Hôm nay',
                  style: TextStyle(
                    color: const Color(0xFF00BFA5),
                    fontWeight: FontWeight.bold,
                    fontSize: 10,
                  ),
                ),
                const SizedBox(width: 4),
                const Icon(
                  Icons.check,
                  color: Color(0xFF00BFA5),
                  size: 10,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendationCard() {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: const Color(0xFF00BFA5).withOpacity(0.12),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF00BFA5).withOpacity(0.30)),
      ),
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: const Color(0xFF00BFA5).withOpacity(0.28),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Center(
              child: Icon(LucideIcons.zap, color: Color(0xFF00BFA5), size: 17),
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
                  'CSS Grid & Flexbox 용어',
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Colors.white.withOpacity(0.92),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  '15 từ vựng cần ôn tập hôm nay',
                  style: TextStyle(
                    fontSize: 11,
                    color: Colors.white.withOpacity(0.50),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          ElevatedButton(
            onPressed: () {},
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF00BFA5),
              foregroundColor: Colors.black,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
              minimumSize: const Size(0, 30),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
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

  Widget _buildProgressCard() {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
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
                  color: Colors.white.withOpacity(0.85),
                ),
              ),
              Row(
                children: [
                  _buildPeriodButton('Tuần', true),
                  const SizedBox(width: 4),
                  _buildPeriodButton('Tháng', false),
                ],
              )
            ],
          ),
          const SizedBox(height: 8),
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
                            case 0: text = 'Nói'; break;
                            case 1: text = 'Từ vựng'; break;
                            case 2: text = 'Nghe'; break;
                            case 3: text = 'Roleplay'; break;
                            default: text = ''; break;
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
                    leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  ),
                  gridData: FlGridData(
                    show: true,
                    drawVerticalLine: false,
                    horizontalInterval: 25,
                    getDrawingHorizontalLine: (value) => FlLine(
                      color: Colors.white.withOpacity(0.05),
                      strokeWidth: 1,
                    ),
                  ),
                  borderData: FlBorderData(show: false),
                  barGroups: [
                    BarChartGroupData(
                      x: 0,
                      barRods: [BarChartRodData(toY: 72, color: const Color(0xFFa78bfa), width: 14, borderRadius: BorderRadius.circular(4))],
                    ),
                    BarChartGroupData(
                      x: 1,
                      barRods: [BarChartRodData(toY: 85, color: const Color(0xFF00BFA5), width: 14, borderRadius: BorderRadius.circular(4))],
                    ),
                    BarChartGroupData(
                      x: 2,
                      barRods: [BarChartRodData(toY: 60, color: const Color(0xFFfb923c), width: 14, borderRadius: BorderRadius.circular(4))],
                    ),
                    BarChartGroupData(
                      x: 3,
                      barRods: [BarChartRodData(toY: 78, color: const Color(0xFF34d399), width: 14, borderRadius: BorderRadius.circular(4))],
                    ),
                  ],
                ),
                swapAnimationDuration: const Duration(milliseconds: 150),
                swapAnimationCurve: Curves.linear,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildSkillStat('Nói', 72, const Color(0xFFa78bfa)),
              _buildSkillStat('Từ vựng', 85, const Color(0xFF00BFA5)),
              _buildSkillStat('Nghe', 60, const Color(0xFFfb923c)),
              _buildSkillStat('Roleplay', 78, const Color(0xFF34d399)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPeriodButton(String text, bool isActive) {
    return Container(
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
    );
  }

  Widget _buildSkillStat(String label, int value, Color color) {
    return Expanded(
      child: Column(
        children: [
          Text(
            value.toString(),
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
        _buildNavCard(context, 'Video Lab', 'Phim kỹ thuật', LucideIcons.play, const Color(0xFFfb923c), '/video'),
        _buildNavCard(context, 'TechTalk AI', 'Roleplay IT', LucideIcons.messageSquare, const Color(0xFFa78bfa), '/techtalk-select'),
        _buildNavCard(context, 'Phát âm', 'Luyện giọng', LucideIcons.mic, const Color(0xFF34d399), '/pronunciation-list'),
        _buildNavCard(context, 'Honorifics', 'Ngữ pháp tôn kính', LucideIcons.target, const Color(0xFFfbbf24), '/honorifics'),
      ],
    );
  }

  Widget _buildNavCard(BuildContext context, String title, String subTitle, IconData icon, Color color, String route) {
    return Container(
      decoration: BoxDecoration(
        color: color.withOpacity(0.10),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withOpacity(0.28)),
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
                    color: color.withOpacity(0.22),
                    borderRadius: BorderRadius.circular(9),
                  ),
                  child: Center(
                    child: Icon(icon, color: color, size: 15),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  title,
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Colors.white.withOpacity(0.90),
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
                    color: Colors.white.withOpacity(0.45),
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

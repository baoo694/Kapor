import 'package:flutter_test/flutter_test.dart';
import 'package:kapor_flutter/features/dashboard/data/dashboard_service.dart';

void main() {
  test(
    'parses dashboard metrics and a recommendation from the API response',
    () {
      final dashboard = DashboardData.fromJson({
        'streak': {'currentStreak': 4, 'longestStreak': 9, 'activeToday': true},
        'progress': {
          'period': 'monthly',
          'hasData': true,
          'activityDays': 6,
          'speaking': 70,
          'vocabulary': 82,
          'listening': 65,
          'roleplayScore': 76,
        },
        'recommendation': {
          'type': 'review_due',
          'title': 'Ôn tập thẻ đến hạn',
          'subtitle': 'Bạn có 3 thẻ cần ôn',
          'targetScreen': '/membyte-review/all',
          'icon': '🧠',
        },
      });

      expect(dashboard.streak.currentStreak, 4);
      expect(dashboard.progress.period, DashboardPeriod.monthly);
      expect(dashboard.progress.vocabulary, 82);
      expect(dashboard.recommendation?.targetScreen, '/membyte-review/all');
    },
  );

  test('keeps zero values as an explicit no-progress state', () {
    final progress = DashboardProgress.fromJson({
      'period': 'weekly',
      'hasData': false,
      'activityDays': 0,
      'speaking': 0,
      'vocabulary': 0,
      'listening': 0,
      'roleplayScore': 0,
    });

    expect(progress.hasData, isFalse);
    expect(progress.speaking, 0);
  });
}

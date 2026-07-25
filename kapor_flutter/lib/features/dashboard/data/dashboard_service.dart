import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

enum DashboardPeriod { weekly, monthly }

extension DashboardPeriodValue on DashboardPeriod {
  String get value => this == DashboardPeriod.weekly ? 'weekly' : 'monthly';
}

class DashboardStreak {
  const DashboardStreak({
    required this.currentStreak,
    required this.longestStreak,
    required this.isActiveToday,
  });

  final int currentStreak;
  final int longestStreak;
  final bool isActiveToday;

  factory DashboardStreak.fromJson(Map<String, dynamic> json) =>
      DashboardStreak(
        currentStreak: (json['currentStreak'] as num?)?.toInt() ?? 0,
        longestStreak: (json['longestStreak'] as num?)?.toInt() ?? 0,
        isActiveToday: json['activeToday'] == true,
      );
}

class DashboardProgress {
  const DashboardProgress({
    required this.period,
    required this.hasData,
    required this.activityDays,
    required this.speaking,
    required this.vocabulary,
    required this.listening,
    required this.roleplay,
  });

  final DashboardPeriod period;
  final bool hasData;
  final int activityDays;
  final int speaking;
  final int vocabulary;
  final int listening;
  final int roleplay;

  factory DashboardProgress.fromJson(Map<String, dynamic> json) {
    return DashboardProgress(
      period: json['period'] == 'monthly'
          ? DashboardPeriod.monthly
          : DashboardPeriod.weekly,
      hasData: json['hasData'] == true,
      activityDays: (json['activityDays'] as num?)?.toInt() ?? 0,
      speaking: (json['speaking'] as num?)?.toInt() ?? 0,
      vocabulary: (json['vocabulary'] as num?)?.toInt() ?? 0,
      listening: (json['listening'] as num?)?.toInt() ?? 0,
      roleplay: (json['roleplayScore'] as num?)?.toInt() ?? 0,
    );
  }
}

class DashboardRecommendation {
  const DashboardRecommendation({
    required this.type,
    required this.title,
    required this.subtitle,
    required this.targetScreen,
    required this.icon,
  });

  final String type;
  final String title;
  final String subtitle;
  final String targetScreen;
  final String icon;

  factory DashboardRecommendation.fromJson(Map<String, dynamic> json) =>
      DashboardRecommendation(
        type: json['type']?.toString() ?? 'explore',
        title: json['title']?.toString() ?? '',
        subtitle: json['subtitle']?.toString() ?? '',
        targetScreen: json['targetScreen']?.toString() ?? '/devvocab',
        icon: json['icon']?.toString() ?? '⚡',
      );
}

class DashboardData {
  const DashboardData({
    required this.streak,
    required this.progress,
    required this.recommendation,
  });

  final DashboardStreak streak;
  final DashboardProgress progress;
  final DashboardRecommendation? recommendation;

  factory DashboardData.fromJson(Map<String, dynamic> json) {
    final streak = json['streak'];
    final progress = json['progress'];
    final recommendation = json['recommendation'];
    if (streak is! Map || progress is! Map) {
      throw const FormatException('Dữ liệu dashboard không hợp lệ.');
    }
    return DashboardData(
      streak: DashboardStreak.fromJson(Map<String, dynamic>.from(streak)),
      progress: DashboardProgress.fromJson(Map<String, dynamic>.from(progress)),
      recommendation: recommendation is Map
          ? DashboardRecommendation.fromJson(
              Map<String, dynamic>.from(recommendation),
            )
          : null,
    );
  }
}

class DashboardService {
  final Dio _dio = ApiClient().dio;

  Future<DashboardData> getDashboard(DashboardPeriod period) async {
    try {
      final response = await _dio.get(
        '/analytics/dashboard',
        queryParameters: {'period': period.value},
      );
      final body = response.data;
      if (body is! Map || body['success'] != true || body['data'] is! Map) {
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể tải dashboard.'
              : 'Không thể tải dashboard.',
        );
      }
      return DashboardData.fromJson(Map<String, dynamic>.from(body['data']));
    } on DioException catch (error) {
      final body = error.response?.data;
      final message = body is Map ? body['message']?.toString() : null;
      throw Exception(message ?? 'Không thể tải dashboard.');
    }
  }
}

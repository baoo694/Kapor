import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

class CorrectionDiff {
  const CorrectionDiff({
    required this.original,
    required this.corrected,
    required this.type,
    required this.explanation,
  });

  final String original;
  final String corrected;
  final String type;
  final String explanation;

  factory CorrectionDiff.fromJson(Map<String, dynamic> json) => CorrectionDiff(
    original: json['original']?.toString() ?? '',
    corrected: json['corrected']?.toString() ?? '',
    type: json['type']?.toString() ?? 'grammar',
    explanation: json['explanation']?.toString() ?? '',
  );
}

class HonorificAnalysis {
  const HonorificAnalysis({
    required this.currentLevel,
    required this.confidence,
    required this.corrections,
    required this.transformedText,
  });

  final String currentLevel;
  final double confidence;
  final List<CorrectionDiff> corrections;
  final String transformedText;

  factory HonorificAnalysis.fromJson(Map<String, dynamic> json) {
    final rawCorrections = json['corrections'];
    return HonorificAnalysis(
      currentLevel: json['currentLevel']?.toString() ?? 'banmal',
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0,
      corrections: rawCorrections is List
          ? rawCorrections
                .whereType<Map>()
                .map(
                  (item) =>
                      CorrectionDiff.fromJson(Map<String, dynamic>.from(item)),
                )
                .toList()
          : const [],
      transformedText: json['transformedText']?.toString() ?? '',
    );
  }
}

class HonorificsService {
  HonorificsService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;

  Future<HonorificAnalysis> analyze(
    String text, {
    String targetLevel = 'hasipsio',
  }) async {
    try {
      final response = await _dio.post(
        '/honorifics/analyze',
        data: {'text': text, 'targetLevel': targetLevel},
      );
      final body = response.data;
      if (body is! Map || body['success'] != true || body['data'] is! Map) {
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể phân tích văn bản.'
              : 'Phản hồi phân tích không hợp lệ.',
        );
      }
      return HonorificAnalysis.fromJson(
        Map<String, dynamic>.from(body['data'] as Map),
      );
    } on DioException catch (error) {
      final data = error.response?.data;
      throw Exception(
        data is Map && data['message'] is String
            ? data['message']
            : 'Không thể phân tích văn bản. Vui lòng thử lại.',
      );
    }
  }
}

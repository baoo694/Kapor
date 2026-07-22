import 'dart:typed_data';
import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class PronunciationSentence {
  const PronunciationSentence({
    required this.text,
    required this.translationVi,
    required this.audioUrl,
    required this.waveform,
  });
  final String text, translationVi, audioUrl;
  final List<double> waveform;
  factory PronunciationSentence.fromJson(Map<String, dynamic> json) =>
      PronunciationSentence(
        text: json['text']?.toString() ?? '',
        translationVi: json['translationVi']?.toString() ?? '',
        audioUrl: json['audioUrl']?.toString() ?? '',
        waveform: json['waveformData'] is List
            ? (json['waveformData'] as List)
                  .whereType<num>()
                  .map((item) => item.toDouble())
                  .toList()
            : const [],
      );
}

class PronunciationExercise {
  const PronunciationExercise({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.domain,
    required this.difficulty,
    required this.sentences,
  });
  final String id, title, titleVi, domain, difficulty;
  final List<PronunciationSentence> sentences;
  factory PronunciationExercise.fromJson(Map<String, dynamic> json) =>
      PronunciationExercise(
        id: json['id']?.toString() ?? '',
        title: json['title']?.toString() ?? '',
        titleVi: json['titleVi']?.toString() ?? '',
        domain: json['domain']?.toString() ?? '',
        difficulty: json['difficulty']?.toString() ?? '',
        sentences: json['sentences'] is List
            ? (json['sentences'] as List)
                  .whereType<Map>()
                  .map(
                    (item) => PronunciationSentence.fromJson(
                      Map<String, dynamic>.from(item),
                    ),
                  )
                  .toList()
            : const [],
      );
}

class PronunciationResult {
  const PronunciationResult({
    required this.status,
    required this.message,
    required this.userWaveform,
  });
  final String status, message;
  final List<double> userWaveform;
  factory PronunciationResult.fromJson(Map<String, dynamic> json) =>
      PronunciationResult(
        status: json['status']?.toString() ?? '',
        message: json['message']?.toString() ?? '',
        userWaveform: json['userWaveform'] is List
            ? (json['userWaveform'] as List)
                  .whereType<num>()
                  .map((item) => item.toDouble())
                  .toList()
            : const [],
      );
}

class PronunciationService {
  PronunciationService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;
  Future<List<PronunciationExercise>> exercises() async {
    final data = await _data(_dio.get('/pronunciation/exercises'));
    if (data is! List)
      throw const FormatException('Danh sách bài phát âm không hợp lệ.');
    return data
        .whereType<Map>()
        .map(
          (item) =>
              PronunciationExercise.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList();
  }

  Future<PronunciationResult> evaluate({
    required String exerciseId,
    required Uint8List bytes,
  }) async {
    final form = FormData.fromMap({
      'exerciseId': exerciseId,
      'sentenceIndex': 0,
      'audioFile': MultipartFile.fromBytes(
        bytes,
        filename: 'attempt.pcm',
        contentType: DioMediaType('audio', 'pcm'),
      ),
    });
    final data = await _data(_dio.post('/pronunciation/evaluate', data: form));
    if (data is! Map)
      throw const FormatException('Kết quả phát âm không hợp lệ.');
    return PronunciationResult.fromJson(Map<String, dynamic>.from(data));
  }

  Future<dynamic> _data(Future<Response<dynamic>> response) async {
    try {
      final body = (await response).data;
      if (body is! Map || body['success'] != true)
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể xử lý phát âm.'
              : 'Phản hồi không hợp lệ.',
        );
      return body['data'];
    } on DioException catch (error) {
      final data = error.response?.data;
      throw Exception(
        data is Map && data['message'] is String
            ? data['message']
            : 'Không thể kết nối máy chủ phát âm.',
      );
    }
  }
}

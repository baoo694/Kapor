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
    required this.attemptId,
    required this.status,
    required this.message,
    required this.scores,
    required this.transcriptionText,
    required this.analysis,
    required this.feedback,
    required this.userWaveform,
    required this.attemptAudioUrl,
  });
  final String attemptId, status, message, transcriptionText, attemptAudioUrl;
  final PronunciationScores? scores;
  final PronunciationAnalysis? analysis;
  final List<PronunciationWordFeedback> feedback;
  final List<double> userWaveform;
  factory PronunciationResult.fromJson(Map<String, dynamic> json) =>
      PronunciationResult(
        attemptId: json['attemptId']?.toString() ?? '',
        status: json['status']?.toString() ?? '',
        message: json['message']?.toString() ?? '',
        scores: json['scores'] is Map
            ? PronunciationScores.fromJson(
                Map<String, dynamic>.from(json['scores'] as Map),
              )
            : null,
        transcriptionText: json['transcriptionText']?.toString() ?? '',
        analysis: json['analysis'] is Map
            ? PronunciationAnalysis.fromJson(
                Map<String, dynamic>.from(json['analysis'] as Map),
              )
            : null,
        feedback: _feedback(json['transcription']),
        userWaveform: json['userWaveform'] is List
            ? (json['userWaveform'] as List)
                  .whereType<num>()
                  .map((item) => item.toDouble())
                  .toList()
            : const [],
        attemptAudioUrl: json['attemptAudioUrl']?.toString() ?? '',
      );

  PronunciationAttemptSummary toHistory() => PronunciationAttemptSummary(
    id: attemptId,
    status: status,
    scores: scores,
    transcriptionText: transcriptionText,
    analysis: analysis,
    feedback: feedback,
    userWaveform: userWaveform,
    attemptAudioUrl: attemptAudioUrl,
    attemptedAt: DateTime.now(),
  );
}

class PronunciationScores {
  const PronunciationScores({
    required this.accuracy,
    required this.fluency,
    required this.completeness,
    required this.overall,
  });
  final int accuracy, fluency, completeness, overall;
  factory PronunciationScores.fromJson(Map<String, dynamic> json) =>
      PronunciationScores(
        accuracy: _int(json['accuracy']),
        fluency: _int(json['fluency']),
        completeness: _int(json['completeness']),
        overall: _int(json['overall']),
      );
}

class PronunciationAnalysis {
  const PronunciationAnalysis({
    required this.summaryVi,
    required this.correctedText,
    required this.grammarNoteVi,
  });

  final String summaryVi, correctedText, grammarNoteVi;

  factory PronunciationAnalysis.fromJson(Map<String, dynamic> json) =>
      PronunciationAnalysis(
        summaryVi: json['summaryVi']?.toString() ?? '',
        correctedText: json['correctedText']?.toString() ?? '',
        grammarNoteVi: json['grammarNoteVi']?.toString() ?? '',
      );
}

class PronunciationWordFeedback {
  const PronunciationWordFeedback({
    required this.text,
    required this.score,
    required this.accuracy,
    required this.phonemeDetail,
  });
  final String text, accuracy, phonemeDetail;
  final int score;
  factory PronunciationWordFeedback.fromJson(Map<String, dynamic> json) =>
      PronunciationWordFeedback(
        text: json['text']?.toString() ?? '',
        score: _int(json['score']),
        accuracy: json['accuracy']?.toString() ?? '',
        phonemeDetail: json['phonemeDetail']?.toString() ?? '',
      );
}

class PronunciationAttemptSummary {
  const PronunciationAttemptSummary({
    required this.id,
    required this.status,
    required this.scores,
    required this.transcriptionText,
    required this.analysis,
    required this.feedback,
    required this.userWaveform,
    required this.attemptAudioUrl,
    required this.attemptedAt,
  });
  final String id, status, transcriptionText, attemptAudioUrl;
  final PronunciationScores? scores;
  final PronunciationAnalysis? analysis;
  final List<PronunciationWordFeedback> feedback;
  final List<double> userWaveform;
  final DateTime? attemptedAt;
  factory PronunciationAttemptSummary.fromJson(Map<String, dynamic> json) =>
      PronunciationAttemptSummary(
        id: json['id']?.toString() ?? '',
        status: json['status']?.toString() ?? '',
        scores: json['scores'] is Map
            ? PronunciationScores.fromJson(
                Map<String, dynamic>.from(json['scores'] as Map),
              )
            : null,
        transcriptionText: json['transcriptionText']?.toString() ?? '',
        analysis: json['analysis'] is Map
            ? PronunciationAnalysis.fromJson(
                Map<String, dynamic>.from(json['analysis'] as Map),
              )
            : null,
        feedback: _feedback(json['transcription']),
        userWaveform: json['userWaveform'] is List
            ? (json['userWaveform'] as List)
                  .whereType<num>()
                  .map((item) => item.toDouble())
                  .toList()
            : const [],
        attemptAudioUrl: json['attemptAudioUrl']?.toString() ?? '',
        attemptedAt: DateTime.tryParse(json['attemptedAt']?.toString() ?? ''),
      );
}

int _int(dynamic value) => value is num ? value.round().clamp(0, 100) : 0;

List<PronunciationWordFeedback> _feedback(dynamic value) => value is List
    ? value
          .whereType<Map>()
          .map(
            (item) => PronunciationWordFeedback.fromJson(
              Map<String, dynamic>.from(item),
            ),
          )
          .toList()
    : const [];

class PronunciationService {
  static const _evaluationReceiveTimeout = Duration(seconds: 90);

  PronunciationService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;
  Future<List<PronunciationExercise>> exercises() async {
    final data = await _data(_dio.get('/pronunciation/exercises'));
    if (data is! List) {
      throw const FormatException('Danh sách bài phát âm không hợp lệ.');
    }
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
    // Whisper transcription plus Gemini analysis can take longer than the
    // short default used by ordinary API requests.
    final data = await _data(
      _dio.post(
        '/pronunciation/evaluate',
        data: form,
        options: Options(receiveTimeout: _evaluationReceiveTimeout),
      ),
    );
    if (data is! Map) {
      throw const FormatException('Kết quả phát âm không hợp lệ.');
    }
    return PronunciationResult.fromJson(Map<String, dynamic>.from(data));
  }

  Future<List<PronunciationAttemptSummary>> history(String exerciseId) async {
    final data = await _data(
      _dio.get(
        '/pronunciation/history',
        queryParameters: {'exerciseId': exerciseId},
      ),
    );
    if (data is! List) {
      throw const FormatException('Lịch sử phát âm không hợp lệ.');
    }
    return data
        .whereType<Map>()
        .map(
          (item) => PronunciationAttemptSummary.fromJson(
            Map<String, dynamic>.from(item),
          ),
        )
        .toList();
  }

  Future<Uint8List> attemptAudio(String attemptAudioUrl) async {
    if (attemptAudioUrl.isEmpty) {
      throw const FormatException('Bản ghi này đã hết hạn lưu trữ.');
    }
    try {
      final response = await _dio.get<List<int>>(
        attemptAudioUrl,
        // ApiClient defaults to application/json, while this endpoint streams
        // WAV on success and JSON only when it needs to report an API error.
        options: Options(
          responseType: ResponseType.bytes,
          headers: {'Accept': 'audio/wav, application/json'},
        ),
      );
      final data = response.data;
      if (data == null || data.isEmpty) {
        throw const FormatException('Không nhận được audio bản ghi.');
      }
      return Uint8List.fromList(data);
    } on DioException catch (error) {
      final data = error.response?.data;
      throw Exception(
        data is Map && data['message'] is String
            ? data['message']
            : 'Không thể tải bản ghi phát âm.',
      );
    }
  }

  Future<dynamic> _data(Future<Response<dynamic>> response) async {
    try {
      final body = (await response).data;
      if (body is! Map || body['success'] != true) {
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể xử lý phát âm.'
              : 'Phản hồi không hợp lệ.',
        );
      }
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

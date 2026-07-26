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
    required this.assessmentVersion,
    required this.assessmentProvider,
    required this.transcriptProvider,
    required this.scores,
    required this.transcriptionText,
    required this.transcript,
    required this.analysis,
    required this.feedback,
    required this.userWaveform,
    required this.attemptAudioUrl,
  });
  final String attemptId,
      status,
      message,
      assessmentVersion,
      assessmentProvider,
      transcriptProvider,
      transcriptionText,
      attemptAudioUrl;
  final PronunciationScores? scores;
  final PronunciationTranscript? transcript;
  final PronunciationAnalysis? analysis;
  final List<PronunciationWordFeedback> feedback;
  final List<double> userWaveform;
  factory PronunciationResult.fromJson(Map<String, dynamic> json) =>
      PronunciationResult(
        attemptId: json['attemptId']?.toString() ?? '',
        status: json['status']?.toString() ?? '',
        message: json['message']?.toString() ?? '',
        assessmentVersion: json['assessmentVersion']?.toString() ?? 'legacy',
        assessmentProvider: json['assessmentProvider']?.toString() ?? 'legacy',
        transcriptProvider: json['transcriptProvider']?.toString() ?? 'legacy',
        scores: json['scores'] is Map
            ? PronunciationScores.fromJson(
                Map<String, dynamic>.from(json['scores'] as Map),
              )
            : null,
        transcriptionText: json['transcriptionText']?.toString() ?? '',
        transcript: json['transcript'] is Map
            ? PronunciationTranscript.fromJson(
                Map<String, dynamic>.from(json['transcript'] as Map),
              )
            : null,
        analysis: json['analysis'] is Map
            ? PronunciationAnalysis.fromJson(
                Map<String, dynamic>.from(json['analysis'] as Map),
              )
            : null,
        feedback: _feedback(json['assessmentWords'] ?? json['transcription']),
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
    transcript: transcript,
    assessmentVersion: assessmentVersion,
    assessmentProvider: assessmentProvider,
    transcriptProvider: transcriptProvider,
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

class PronunciationTranscript {
  const PronunciationTranscript({
    required this.provider,
    required this.text,
    required this.durationSeconds,
    required this.words,
  });
  final String provider, text;
  final double durationSeconds;
  final List<PronunciationTranscriptWord> words;
  factory PronunciationTranscript.fromJson(Map<String, dynamic> json) =>
      PronunciationTranscript(
        provider: json['provider']?.toString() ?? 'whisperx',
        text: json['text']?.toString() ?? '',
        durationSeconds: _double(json['durationSeconds']),
        words: json['words'] is List
            ? (json['words'] as List)
                  .whereType<Map>()
                  .map(
                    (item) => PronunciationTranscriptWord.fromJson(
                      Map<String, dynamic>.from(item),
                    ),
                  )
                  .toList()
            : const [],
      );
}

class PronunciationTranscriptWord {
  const PronunciationTranscriptWord({
    required this.text,
    required this.startSeconds,
    required this.endSeconds,
    required this.confidence,
    required this.timingStatus,
  });
  final String text, timingStatus;
  final double? startSeconds, endSeconds, confidence;
  factory PronunciationTranscriptWord.fromJson(Map<String, dynamic> json) =>
      PronunciationTranscriptWord(
        text: json['text']?.toString() ?? '',
        startSeconds: _nullableDouble(json['startSeconds']),
        endSeconds: _nullableDouble(json['endSeconds']),
        confidence: _nullableDouble(json['confidence']),
        timingStatus: json['timingStatus']?.toString() ?? 'unaligned',
      );
}

class PronunciationAnalysis {
  const PronunciationAnalysis({
    required this.provider,
    required this.status,
    required this.summaryVi,
    required this.correctedText,
    required this.grammarNoteVi,
    required this.interpretations,
  });

  final String provider, status, summaryVi, correctedText, grammarNoteVi;
  final List<PronunciationInterpretation> interpretations;

  factory PronunciationAnalysis.fromJson(Map<String, dynamic> json) =>
      PronunciationAnalysis(
        provider: json['provider']?.toString() ?? 'legacy',
        status: json['status']?.toString() ?? 'completed',
        summaryVi: json['summaryVi']?.toString() ?? '',
        correctedText: json['correctedText']?.toString() ?? '',
        grammarNoteVi: json['grammarNoteVi']?.toString() ?? '',
        interpretations: json['interpretations'] is List
            ? (json['interpretations'] as List)
                  .whereType<Map>()
                  .map(
                    (item) => PronunciationInterpretation.fromJson(
                      Map<String, dynamic>.from(item),
                    ),
                  )
                  .toList()
            : const [],
      );
}

class PronunciationInterpretation {
  const PronunciationInterpretation({
    required this.wordIndex,
    required this.explanationVi,
    required this.practiceTipVi,
  });
  final int wordIndex;
  final String explanationVi, practiceTipVi;
  factory PronunciationInterpretation.fromJson(Map<String, dynamic> json) =>
      PronunciationInterpretation(
        wordIndex: _int(json['wordIndex']),
        explanationVi: json['explanationVi']?.toString() ?? '',
        practiceTipVi: json['practiceTipVi']?.toString() ?? '',
      );
}

class PronunciationWordFeedback {
  const PronunciationWordFeedback({
    required this.text,
    required this.score,
    required this.accuracy,
    required this.phonemeDetail,
    required this.errorType,
    required this.phonemes,
  });
  final String text, accuracy, phonemeDetail, errorType;
  final int score;
  final List<PronunciationPhonemeFeedback> phonemes;
  factory PronunciationWordFeedback.fromJson(Map<String, dynamic> json) =>
      PronunciationWordFeedback(
        text: json['text']?.toString() ?? '',
        score: _int(json['score']),
        accuracy: json['accuracy']?.toString() ?? '',
        phonemeDetail: json['phonemeDetail']?.toString() ?? '',
        errorType: json['errorType']?.toString() ?? 'None',
        phonemes: _phonemes(json['phonemes']),
      );
}

class PronunciationPhonemeFeedback {
  const PronunciationPhonemeFeedback({
    required this.index,
    required this.score,
  });
  final int index, score;
  factory PronunciationPhonemeFeedback.fromJson(Map<String, dynamic> json) =>
      PronunciationPhonemeFeedback(
        index: _int(json['index']),
        score: _int(json['score']),
      );
}

class PronunciationAttemptSummary {
  const PronunciationAttemptSummary({
    required this.id,
    required this.status,
    required this.scores,
    required this.transcriptionText,
    required this.transcript,
    required this.assessmentVersion,
    required this.assessmentProvider,
    required this.transcriptProvider,
    required this.analysis,
    required this.feedback,
    required this.userWaveform,
    required this.attemptAudioUrl,
    required this.attemptedAt,
  });
  final String id,
      status,
      transcriptionText,
      assessmentVersion,
      assessmentProvider,
      transcriptProvider,
      attemptAudioUrl;
  final PronunciationScores? scores;
  final PronunciationTranscript? transcript;
  final PronunciationAnalysis? analysis;
  final List<PronunciationWordFeedback> feedback;
  final List<double> userWaveform;
  final DateTime? attemptedAt;
  factory PronunciationAttemptSummary.fromJson(Map<String, dynamic> json) =>
      PronunciationAttemptSummary(
        id: json['id']?.toString() ?? '',
        status: json['status']?.toString() ?? '',
        assessmentVersion: json['assessmentVersion']?.toString() ?? 'legacy',
        assessmentProvider: json['assessmentProvider']?.toString() ?? 'legacy',
        transcriptProvider: json['transcriptProvider']?.toString() ?? 'legacy',
        scores: json['scores'] is Map
            ? PronunciationScores.fromJson(
                Map<String, dynamic>.from(json['scores'] as Map),
              )
            : null,
        transcriptionText: json['transcriptionText']?.toString() ?? '',
        transcript: json['transcript'] is Map
            ? PronunciationTranscript.fromJson(
                Map<String, dynamic>.from(json['transcript'] as Map),
              )
            : null,
        analysis: json['analysis'] is Map
            ? PronunciationAnalysis.fromJson(
                Map<String, dynamic>.from(json['analysis'] as Map),
              )
            : null,
        feedback: _feedback(json['assessmentWords'] ?? json['transcription']),
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

int _int(dynamic value) =>
    value is num ? value.round().clamp(0, 100).toInt() : 0;
double _double(dynamic value) => value is num ? value.toDouble() : 0;
double? _nullableDouble(dynamic value) =>
    value is num ? value.toDouble() : null;

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

List<PronunciationPhonemeFeedback> _phonemes(dynamic value) => value is List
    ? value
          .whereType<Map>()
          .map(
            (item) => PronunciationPhonemeFeedback.fromJson(
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
    // Azure PA and WhisperX run in parallel; Gemini only renders Vietnamese
    // explanations after both evidence sources complete.
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

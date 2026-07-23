import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

class VideoToken {
  const VideoToken({
    required this.surface,
    required this.stem,
    required this.pronunciation,
    required this.meaningVi,
    required this.meaningEn,
    required this.definitionEn,
    required this.exampleKo,
    required this.clickable,
  });
  final String surface;
  final String stem;
  final String pronunciation;
  final String meaningVi;
  final String meaningEn;
  final String definitionEn;
  final String exampleKo;
  final bool clickable;
  factory VideoToken.fromJson(Map<String, dynamic> json) => VideoToken(
    surface: json['surface']?.toString() ?? '',
    stem: json['stem']?.toString() ?? '',
    pronunciation: json['pronunciation']?.toString() ?? '',
    meaningVi: json['meaningVi']?.toString() ?? '',
    meaningEn: json['meaningEn']?.toString() ?? '',
    definitionEn: json['definitionEn']?.toString() ?? '',
    exampleKo: json['exampleKo']?.toString() ?? '',
    clickable: json['clickable'] == true,
  );
}

class VideoSubtitle {
  const VideoSubtitle({
    required this.start,
    required this.end,
    required this.text,
    required this.tokens,
  });
  final double start;
  final double end;
  final String text;
  final List<VideoToken> tokens;
  factory VideoSubtitle.fromJson(Map<String, dynamic> json) => VideoSubtitle(
    start: (json['start'] as num?)?.toDouble() ?? 0,
    end: (json['end'] as num?)?.toDouble() ?? 0,
    text: json['text']?.toString() ?? '',
    tokens: json['tokens'] is List
        ? (json['tokens'] as List)
              .whereType<Map>()
              .map(
                (item) => VideoToken.fromJson(Map<String, dynamic>.from(item)),
              )
              .toList()
        : const [],
  );
}

class VideoQuiz {
  const VideoQuiz({
    required this.id,
    required this.timestamp,
    required this.question,
    required this.questionVi,
    required this.options,
  });
  final String id;
  final double timestamp;
  final String question;
  final String questionVi;
  final List<String> options;
  factory VideoQuiz.fromJson(Map<String, dynamic> json) => VideoQuiz(
    id: json['id']?.toString() ?? '',
    timestamp: (json['timestamp'] as num?)?.toDouble() ?? 0,
    question: json['question']?.toString() ?? '',
    questionVi: json['questionVi']?.toString() ?? '',
    options: json['options'] is List
        ? (json['options'] as List).map((item) => item.toString()).toList()
        : const [],
  );
}

class LearningVideo {
  const LearningVideo({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.youtubeVideoId,
    required this.domain,
    required this.koreanSubtitles,
    required this.vietnameseSubtitles,
    required this.quizzes,
  });
  final String id;
  final String title;
  final String titleVi;
  final String youtubeVideoId;
  final String domain;
  final List<VideoSubtitle> koreanSubtitles;
  final List<VideoSubtitle> vietnameseSubtitles;
  final List<VideoQuiz> quizzes;
  factory LearningVideo.fromJson(Map<String, dynamic> json) => LearningVideo(
    id: json['id']?.toString() ?? '',
    title: json['title']?.toString() ?? '',
    titleVi: json['titleVi']?.toString() ?? '',
    youtubeVideoId: json['youtubeVideoId']?.toString() ?? '',
    domain: json['domain']?.toString() ?? '',
    koreanSubtitles: _subtitles(json['koreanSubtitles']),
    vietnameseSubtitles: _subtitles(json['vietnameseSubtitles']),
    quizzes: _quizzes(json['quizMarkers']),
  );
  static List<VideoSubtitle> _subtitles(dynamic source) => source is List
      ? source
            .whereType<Map>()
            .map(
              (item) => VideoSubtitle.fromJson(Map<String, dynamic>.from(item)),
            )
            .toList()
      : const [];
  static List<VideoQuiz> _quizzes(dynamic source) => source is List
      ? source
            .whereType<Map>()
            .map((item) => VideoQuiz.fromJson(Map<String, dynamic>.from(item)))
            .toList()
      : const [];
}

class VideoService {
  VideoService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;
  Future<List<LearningVideo>> getVideos() async {
    try {
      final response = await _dio.get('/videos');
      final body = response.data;
      if (body is! Map || body['success'] != true || body['data'] is! List)
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể tải video.'
              : 'Phản hồi video không hợp lệ.',
        );
      return (body['data'] as List)
          .whereType<Map>()
          .map(
            (item) => LearningVideo.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList();
    } on DioException catch (error) {
      final data = error.response?.data;
      throw Exception(
        data is Map && data['message'] is String
            ? data['message']
            : 'Không thể tải video. Vui lòng thử lại.',
      );
    }
  }

  Future<bool> answerQuiz(String videoId, String quizId, int answer) async {
    final response = await _dio.post(
      '/videos/$videoId/quiz/$quizId/answer',
      data: {'answer': answer},
    );
    final body = response.data;
    return body is Map &&
        body['success'] == true &&
        body['data'] is Map &&
        body['data']['correct'] == true;
  }
}

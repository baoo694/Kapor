import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

class DevVocabTopic {
  final String id;
  final String title;
  final String titleVi;
  final String domain;
  final bool isLocked;
  final double completionPercent;
  final int totalLessons;
  final int completedLessons;

  const DevVocabTopic({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.domain,
    required this.isLocked,
    required this.completionPercent,
    required this.totalLessons,
    required this.completedLessons,
  });

  factory DevVocabTopic.fromJson(Map<String, dynamic> json) {
    return DevVocabTopic(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      titleVi: json['titleVi']?.toString() ?? '',
      domain: json['domain']?.toString() ?? '',
      // Spring/Jackson commonly serializes `isLocked` as `locked`; accept both
      // forms so the mobile app remains compatible with either representation.
      isLocked: json['isLocked'] == true || json['locked'] == true,
      completionPercent: (json['completionPercent'] as num?)?.toDouble() ?? 0,
      totalLessons: (json['totalLessons'] as num?)?.toInt() ?? 0,
      completedLessons: (json['completedLessons'] as num?)?.toInt() ?? 0,
    );
  }
}

class LessonVocabularyItem {
  final String id;
  final String korean;
  final String pronunciation;
  final String vietnamese;
  final String english;
  final String context;
  final String codeSnippet;
  final String audioUrl;

  const LessonVocabularyItem({
    required this.id,
    required this.korean,
    required this.pronunciation,
    required this.vietnamese,
    required this.english,
    required this.context,
    required this.codeSnippet,
    required this.audioUrl,
  });

  factory LessonVocabularyItem.fromJson(Map<String, dynamic> json) {
    return LessonVocabularyItem(
      id: json['id']?.toString() ?? '',
      korean: json['korean']?.toString() ?? '',
      pronunciation: json['pronunciation']?.toString() ?? '',
      vietnamese: json['vietnamese']?.toString() ?? '',
      english: json['english']?.toString() ?? '',
      context: json['context']?.toString() ?? '',
      codeSnippet: json['codeSnippet']?.toString() ?? '',
      audioUrl: json['audioUrl']?.toString() ?? '',
    );
  }
}

class DevVocabLesson {
  final String id;
  final String topicId;
  final String title;
  final String titleVi;
  final String content;
  final String contentVi;
  final int order;
  final List<LessonVocabularyItem> vocabulary;

  const DevVocabLesson({
    required this.id,
    required this.topicId,
    required this.title,
    required this.titleVi,
    required this.content,
    required this.contentVi,
    required this.order,
    required this.vocabulary,
  });

  factory DevVocabLesson.fromJson(Map<String, dynamic> json) {
    final rawVocabulary = json['vocabulary'];
    return DevVocabLesson(
      id: json['id']?.toString() ?? '',
      topicId: json['topicId']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      titleVi: json['titleVi']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      contentVi: json['contentVi']?.toString() ?? '',
      order: (json['order'] as num?)?.toInt() ?? 0,
      vocabulary: rawVocabulary is List
          ? rawVocabulary
                .whereType<Map>()
                .map(
                  (item) => LessonVocabularyItem.fromJson(
                    Map<String, dynamic>.from(item),
                  ),
                )
                .toList()
          : const [],
    );
  }
}

class FlashcardProgress {
  final String lessonId;
  final int totalCards;
  final int knownCards;
  final int learningCards;
  final Map<String, String> cardStatuses;

  const FlashcardProgress({
    required this.lessonId,
    required this.totalCards,
    required this.knownCards,
    required this.learningCards,
    required this.cardStatuses,
  });

  factory FlashcardProgress.fromJson(Map<String, dynamic> json) {
    final rawStatuses = json['cardStatuses'];
    return FlashcardProgress(
      lessonId: json['lessonId']?.toString() ?? '',
      totalCards: (json['totalCards'] as num?)?.toInt() ?? 0,
      knownCards: (json['knownCards'] as num?)?.toInt() ?? 0,
      learningCards: (json['learningCards'] as num?)?.toInt() ?? 0,
      cardStatuses: rawStatuses is Map
          ? rawStatuses.map(
              (key, value) => MapEntry(key.toString(), value.toString()),
            )
          : const {},
    );
  }
}

class DevVocabService {
  final Dio _dio = ApiClient().dio;

  Future<List<DevVocabTopic>> getTopics(String domain) async {
    try {
      final response = await _dio.get(
        '/topics',
        queryParameters: {'domain': domain},
      );
      final responseBody = response.data;

      if (responseBody is! Map<String, dynamic>) {
        throw const FormatException('Phản hồi Topics không hợp lệ.');
      }
      if (responseBody['success'] != true) {
        throw Exception(responseBody['message'] ?? 'Không thể tải Topics.');
      }

      final data = responseBody['data'];
      if (data is! List) {
        throw const FormatException('Dữ liệu Topics không hợp lệ.');
      }

      return data
          .whereType<Map>()
          .map(
            (item) => DevVocabTopic.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList();
    } on DioException catch (error) {
      final data = error.response?.data;
      if (data is Map && data['message'] is String) {
        throw Exception(data['message']);
      }
      throw Exception('Không thể tải Topics. Vui lòng kiểm tra kết nối mạng.');
    }
  }

  Future<List<DevVocabLesson>> getLessons(String topicId) async {
    try {
      final response = await _dio.get(
        '/lessons',
        queryParameters: {'topicId': topicId},
      );
      final responseBody = response.data;

      if (responseBody is! Map<String, dynamic>) {
        throw const FormatException('Phản hồi Lessons không hợp lệ.');
      }
      if (responseBody['success'] != true) {
        throw Exception(responseBody['message'] ?? 'Không thể tải Lessons.');
      }

      final data = responseBody['data'];
      if (data is! List) {
        throw const FormatException('Dữ liệu Lessons không hợp lệ.');
      }

      final lessons = data
          .whereType<Map>()
          .map(
            (item) => DevVocabLesson.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList();
      lessons.sort((first, second) => first.order.compareTo(second.order));
      return lessons;
    } on DioException catch (error) {
      final data = error.response?.data;
      if (data is Map && data['message'] is String) {
        throw Exception(data['message']);
      }
      throw Exception('Không thể tải Lessons. Vui lòng kiểm tra kết nối mạng.');
    }
  }

  Future<DevVocabLesson> getLesson(String lessonId) async {
    try {
      final response = await _dio.get('/lessons/$lessonId');
      final responseBody = response.data;
      if (responseBody is! Map<String, dynamic> ||
          responseBody['data'] is! Map) {
        throw const FormatException('Phản hồi Lesson không hợp lệ.');
      }
      if (responseBody['success'] != true) {
        throw Exception(responseBody['message'] ?? 'Không thể tải Lesson.');
      }
      return DevVocabLesson.fromJson(
        Map<String, dynamic>.from(responseBody['data'] as Map),
      );
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải Lesson.'));
    }
  }

  Future<FlashcardProgress> getFlashcardProgress(String lessonId) async {
    try {
      final response = await _dio.get('/lessons/$lessonId/flashcards/progress');
      return _flashcardProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải tiến độ thẻ.'));
    }
  }

  Future<FlashcardProgress> updateFlashcardStatus({
    required String lessonId,
    required String vocabularyId,
    required String status,
  }) async {
    try {
      final response = await _dio.put(
        '/lessons/$lessonId/flashcards/$vocabularyId',
        data: {'status': status},
      );
      return _flashcardProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể lưu tiến độ thẻ.'));
    }
  }

  Future<FlashcardProgress> resetFlashcardProgress(String lessonId) async {
    try {
      final response = await _dio.delete(
        '/lessons/$lessonId/flashcards/progress',
      );
      return _flashcardProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể đặt lại tiến độ thẻ.'),
      );
    }
  }

  FlashcardProgress _flashcardProgressFromResponse(dynamic responseBody) {
    if (responseBody is! Map<String, dynamic> || responseBody['data'] is! Map) {
      throw const FormatException('Phản hồi tiến độ thẻ không hợp lệ.');
    }
    if (responseBody['success'] != true) {
      throw Exception(
        responseBody['message'] ?? 'Không thể xử lý tiến độ thẻ.',
      );
    }
    return FlashcardProgress.fromJson(
      Map<String, dynamic>.from(responseBody['data'] as Map),
    );
  }

  String _messageFromError(DioException error, String fallback) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return data['message'] as String;
    }
    return fallback;
  }
}

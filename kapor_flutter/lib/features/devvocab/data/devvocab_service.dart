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

class LessonExercise {
  final String id;
  final String type;
  final String question;
  final String questionVi;
  final List<String> options;

  const LessonExercise({
    required this.id,
    required this.type,
    required this.question,
    required this.questionVi,
    required this.options,
  });

  factory LessonExercise.fromJson(Map<String, dynamic> json) {
    final rawOptions = json['options'];
    return LessonExercise(
      id: json['id']?.toString() ?? '',
      type: json['type']?.toString() ?? '',
      question: json['question']?.toString() ?? '',
      questionVi: json['questionVi']?.toString() ?? '',
      options: rawOptions is List
          ? rawOptions.map((option) => option.toString()).toList()
          : const [],
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
  final List<LessonExercise> exercises;

  const DevVocabLesson({
    required this.id,
    required this.topicId,
    required this.title,
    required this.titleVi,
    required this.content,
    required this.contentVi,
    required this.order,
    required this.vocabulary,
    required this.exercises,
  });

  factory DevVocabLesson.fromJson(Map<String, dynamic> json) {
    final rawVocabulary = json['vocabulary'];
    final rawExercises = json['exercises'];
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
      exercises: rawExercises is List
          ? rawExercises
                .whereType<Map>()
                .map(
                  (item) =>
                      LessonExercise.fromJson(Map<String, dynamic>.from(item)),
                )
                .toList()
          : const [],
    );
  }
}

class LessonActivityProgress {
  final String lessonId;
  final bool studyCompleted;
  final bool quizPassed;
  final bool lessonCompleted;
  final int bestQuizScore;
  final int quizAttempts;
  final int bestMatchAccuracy;
  final int matchingAttempts;

  const LessonActivityProgress({
    required this.lessonId,
    required this.studyCompleted,
    required this.quizPassed,
    required this.lessonCompleted,
    required this.bestQuizScore,
    required this.quizAttempts,
    required this.bestMatchAccuracy,
    required this.matchingAttempts,
  });

  factory LessonActivityProgress.fromJson(Map<String, dynamic> json) {
    return LessonActivityProgress(
      lessonId: json['lessonId']?.toString() ?? '',
      studyCompleted: json['studyCompleted'] == true,
      quizPassed: json['quizPassed'] == true,
      lessonCompleted: json['lessonCompleted'] == true,
      bestQuizScore: (json['bestQuizScore'] as num?)?.toInt() ?? 0,
      quizAttempts: (json['quizAttempts'] as num?)?.toInt() ?? 0,
      bestMatchAccuracy: (json['bestMatchAccuracy'] as num?)?.toInt() ?? 0,
      matchingAttempts: (json['matchingAttempts'] as num?)?.toInt() ?? 0,
    );
  }
}

class QuizResult {
  final int score;
  final int correctAnswers;
  final int totalQuestions;
  final bool passed;
  final LessonActivityProgress progress;

  const QuizResult({
    required this.score,
    required this.correctAnswers,
    required this.totalQuestions,
    required this.passed,
    required this.progress,
  });

  factory QuizResult.fromJson(Map<String, dynamic> json) {
    return QuizResult(
      score: (json['score'] as num?)?.toInt() ?? 0,
      correctAnswers: (json['correctAnswers'] as num?)?.toInt() ?? 0,
      totalQuestions: (json['totalQuestions'] as num?)?.toInt() ?? 0,
      passed: json['passed'] == true,
      progress: LessonActivityProgress.fromJson(
        Map<String, dynamic>.from(json['progress'] as Map? ?? const {}),
      ),
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

class SummarizerCard {
  final String korean;
  final String pronunciation;
  final String vietnamese;
  final String english;
  final String definitionEn;
  final String exampleKo;
  final String grammarNote;
  final String context;

  const SummarizerCard({
    required this.korean,
    required this.pronunciation,
    required this.vietnamese,
    required this.english,
    required this.definitionEn,
    required this.exampleKo,
    required this.grammarNote,
    required this.context,
  });

  factory SummarizerCard.fromJson(Map<String, dynamic> json) => SummarizerCard(
    korean: json['korean']?.toString() ?? '',
    pronunciation: json['pronunciation']?.toString() ?? '',
    vietnamese: json['vietnamese']?.toString() ?? '',
    english: json['english']?.toString() ?? '',
    definitionEn: json['definitionEn']?.toString() ?? '',
    exampleKo: json['exampleKo']?.toString() ?? '',
    grammarNote: json['grammarNote']?.toString() ?? '',
    context: json['context']?.toString() ?? '',
  );

  Map<String, dynamic> toJson() => {
    'korean': korean,
    'pronunciation': pronunciation,
    'vietnamese': vietnamese,
    'english': english,
    'definitionEn': definitionEn,
    'exampleKo': exampleKo,
    'grammarNote': grammarNote,
    'context': context,
  };
}

class SummarizerPreview {
  final String sourceType;
  final String sourceUrl;
  final String title;
  final String sourceExcerpt;
  final List<SummarizerCard> cards;

  const SummarizerPreview({
    required this.sourceType,
    required this.sourceUrl,
    required this.title,
    required this.sourceExcerpt,
    required this.cards,
  });

  factory SummarizerPreview.fromJson(Map<String, dynamic> json) {
    final rawCards = json['cards'];
    return SummarizerPreview(
      sourceType: json['sourceType']?.toString() ?? 'TEXT',
      sourceUrl: json['sourceUrl']?.toString() ?? '',
      title: json['title']?.toString() ?? 'AI Summary',
      sourceExcerpt: json['sourceExcerpt']?.toString() ?? '',
      cards: rawCards is List
          ? rawCards
                .whereType<Map>()
                .map(
                  (item) =>
                      SummarizerCard.fromJson(Map<String, dynamic>.from(item)),
                )
                .toList()
          : const [],
    );
  }
}

class SummarizerSavedDeck {
  final String deckId;
  final int savedCards;

  const SummarizerSavedDeck({required this.deckId, required this.savedCards});

  factory SummarizerSavedDeck.fromJson(Map<String, dynamic> json) =>
      SummarizerSavedDeck(
        deckId: json['deckId']?.toString() ?? '',
        savedCards: (json['savedCards'] as num?)?.toInt() ?? 0,
      );
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

  Future<FlashcardProgress> resetFlashcardStatus({
    required String lessonId,
    required String vocabularyId,
  }) async {
    try {
      final response = await _dio.delete(
        '/lessons/$lessonId/flashcards/$vocabularyId',
      );
      return _flashcardProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể đặt lại trạng thái thẻ.'),
      );
    }
  }

  Future<SummarizerPreview> generateFlashcards(String input) async {
    try {
      final response = await _dio.post(
        '/summarizer/generate',
        data: {'input': input, 'maxCards': 8},
      );
      return SummarizerPreview.fromJson(
        _responseData(response.data, 'Không thể tạo flashcard.'),
      );
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tạo flashcard.'));
    }
  }

  Future<SummarizerSavedDeck> saveSummarizerDeck({
    required SummarizerPreview preview,
    required String title,
    required List<SummarizerCard> cards,
  }) async {
    try {
      final response = await _dio.post(
        '/summarizer/decks',
        data: {
          'title': title,
          'sourceUrl': preview.sourceUrl,
          'sourceTitle': preview.title,
          'sourceExcerpt': preview.sourceExcerpt,
          'cards': cards.map((card) => card.toJson()).toList(),
        },
      );
      return SummarizerSavedDeck.fromJson(
        _responseData(response.data, 'Không thể lưu deck flashcard.'),
      );
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể lưu deck flashcard.'),
      );
    }
  }

  Future<LessonActivityProgress> getActivityProgress(String lessonId) async {
    try {
      final response = await _dio.get('/lessons/$lessonId/activity-progress');
      return _activityProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể tải tiến độ lesson.'),
      );
    }
  }

  Future<LessonActivityProgress> completeStudy(String lessonId) async {
    try {
      final response = await _dio.post('/lessons/$lessonId/study/complete');
      return _activityProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể hoàn thành phần Học.'),
      );
    }
  }

  Future<QuizResult> submitQuiz({
    required String lessonId,
    required Map<String, String> answers,
  }) async {
    try {
      final response = await _dio.post(
        '/lessons/$lessonId/quiz/attempts',
        data: {'answers': answers},
      );
      final body = _responseData(response.data, 'Không thể nộp bài kiểm tra.');
      return QuizResult.fromJson(body);
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể nộp bài kiểm tra.'));
    }
  }

  Future<LessonActivityProgress> recordMatchingAttempt({
    required String lessonId,
    required int completedPairs,
    required int mistakes,
    required int durationSeconds,
  }) async {
    try {
      final response = await _dio.post(
        '/lessons/$lessonId/matching/attempts',
        data: {
          'completedPairs': completedPairs,
          'mistakes': mistakes,
          'durationSeconds': durationSeconds,
        },
      );
      return _activityProgressFromResponse(response.data);
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể lưu kết quả ghép thẻ.'),
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

  LessonActivityProgress _activityProgressFromResponse(dynamic responseBody) {
    return LessonActivityProgress.fromJson(
      _responseData(responseBody, 'Không thể xử lý tiến độ lesson.'),
    );
  }

  Map<String, dynamic> _responseData(dynamic responseBody, String fallback) {
    if (responseBody is! Map<String, dynamic> || responseBody['data'] is! Map) {
      throw FormatException(fallback);
    }
    if (responseBody['success'] != true) {
      throw Exception(responseBody['message'] ?? fallback);
    }
    return Map<String, dynamic>.from(responseBody['data'] as Map);
  }

  String _messageFromError(DioException error, String fallback) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return data['message'] as String;
    }
    return fallback;
  }
}

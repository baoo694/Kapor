import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

class MemByteDeck {
  final String id;
  final String title;
  final String titleVi;
  final String domain;
  final int totalCards;
  final int dueCards;
  final int newCards;

  const MemByteDeck({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.domain,
    required this.totalCards,
    required this.dueCards,
    required this.newCards,
  });

  factory MemByteDeck.fromJson(Map<String, dynamic> json) => MemByteDeck(
    id: json['id']?.toString() ?? '',
    title: json['title']?.toString() ?? '',
    titleVi: json['titleVi']?.toString() ?? '',
    domain: json['domain']?.toString() ?? '',
    totalCards: (json['totalCards'] as num?)?.toInt() ?? 0,
    dueCards: (json['dueCards'] as num?)?.toInt() ?? 0,
    newCards: (json['newCards'] as num?)?.toInt() ?? 0,
  );
}

class MemByteReviewSummary {
  final int totalDueCards;
  final int decksWithDueCards;

  const MemByteReviewSummary({
    required this.totalDueCards,
    required this.decksWithDueCards,
  });

  factory MemByteReviewSummary.fromJson(Map<String, dynamic> json) =>
      MemByteReviewSummary(
        totalDueCards: (json['totalDueCards'] as num?)?.toInt() ?? 0,
        decksWithDueCards: (json['decksWithDueCards'] as num?)?.toInt() ?? 0,
      );
}

class MemByteReviewCard {
  final String id;
  final String deckId;
  final String korean;
  final String pronunciation;
  final String vietnamese;
  final String english;
  final String definitionEn;
  final String exampleKo;
  final String grammarNote;
  final String context;
  final String codeSnippet;
  final String audioUrl;
  final Map<String, String> nextReviewTimes;

  const MemByteReviewCard({
    required this.id,
    required this.deckId,
    required this.korean,
    required this.pronunciation,
    required this.vietnamese,
    required this.english,
    required this.definitionEn,
    required this.exampleKo,
    required this.grammarNote,
    required this.context,
    required this.codeSnippet,
    required this.audioUrl,
    required this.nextReviewTimes,
  });

  factory MemByteReviewCard.fromJson(Map<String, dynamic> json) {
    final rawTimes = json['nextReviewTimes'];
    return MemByteReviewCard(
      id: json['id']?.toString() ?? '',
      deckId: json['deckId']?.toString() ?? '',
      korean: json['korean']?.toString() ?? '',
      pronunciation: json['pronunciation']?.toString() ?? '',
      vietnamese: json['vietnamese']?.toString() ?? '',
      english: json['english']?.toString() ?? '',
      definitionEn: json['definitionEn']?.toString() ?? '',
      exampleKo: json['exampleKo']?.toString() ?? '',
      grammarNote: json['grammarNote']?.toString() ?? '',
      context: json['context']?.toString() ?? '',
      codeSnippet: json['codeSnippet']?.toString() ?? '',
      audioUrl: json['audioUrl']?.toString() ?? '',
      nextReviewTimes: rawTimes is Map
          ? rawTimes.map(
              (key, value) => MapEntry(key.toString(), value.toString()),
            )
          : const {},
    );
  }
}

class MemByteService {
  final Dio _dio = ApiClient().dio;

  Future<void> saveVocabulary({
    required String lessonId,
    required String vocabularyId,
  }) async {
    try {
      await _dio.post('/membyte/lessons/$lessonId/flashcards/$vocabularyId');
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể thêm thẻ vào MemByte.'),
      );
    }
  }

  Future<Set<String>> getSavedVocabularyIds(String lessonId) async {
    try {
      final data = _unwrap(
        await _dio.get('/membyte/lessons/$lessonId/saved-flashcards'),
      );
      if (data is! Map) {
        throw const FormatException('Dữ liệu flashcard đã lưu không hợp lệ.');
      }
      final vocabularyIds = data['vocabularyIds'];
      if (vocabularyIds is! List) return const {};
      return vocabularyIds.map((id) => id.toString()).toSet();
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải thẻ đã lưu.'));
    }
  }

  Future<List<MemByteDeck>> getDecks() async {
    try {
      final data = _unwrap(await _dio.get('/membyte/decks'));
      if (data is! List) {
        throw const FormatException('Dữ liệu bộ thẻ không hợp lệ.');
      }
      return data
          .whereType<Map>()
          .map((item) => MemByteDeck.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải MemByte.'));
    }
  }

  Future<MemByteReviewSummary> getReviewSummary() async {
    try {
      final data = _unwrap(await _dio.get('/membyte/review/summary'));
      if (data is! Map) {
        throw const FormatException('Dữ liệu tổng ôn không hợp lệ.');
      }
      return MemByteReviewSummary.fromJson(Map<String, dynamic>.from(data));
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải tổng ôn.'));
    }
  }

  Future<List<MemByteReviewCard>> getReviewCards({
    String? deckId,
    required bool includeNew,
  }) async {
    try {
      final data = _unwrap(
        await _dio.get(
          '/membyte/review/cards',
          queryParameters: {
            if (deckId != null && deckId != 'all') 'deckId': deckId,
            'includeNew': includeNew,
            'limit': 50,
          },
        ),
      );
      if (data is! List) {
        throw const FormatException('Dữ liệu thẻ ôn không hợp lệ.');
      }
      return data
          .whereType<Map>()
          .map(
            (item) =>
                MemByteReviewCard.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList();
    } on DioException catch (error) {
      throw Exception(_messageFromError(error, 'Không thể tải phiên ôn tập.'));
    }
  }

  Future<void> rateCard({
    required String cardId,
    required String rating,
    required int responseTimeMs,
  }) async {
    try {
      await _dio.post(
        '/membyte/review/rate',
        data: {
          'cardId': cardId,
          'rating': rating,
          'responseTimeMs': responseTimeMs,
        },
      );
    } on DioException catch (error) {
      throw Exception(
        _messageFromError(error, 'Không thể lưu kết quả ôn tập.'),
      );
    }
  }

  dynamic _unwrap(Response<dynamic> response) {
    final body = response.data;
    if (body is! Map<String, dynamic> || body['success'] != true) {
      final message = body is Map ? body['message'] : null;
      throw Exception(message ?? 'Phản hồi MemByte không hợp lệ.');
    }
    return body['data'];
  }

  String _messageFromError(DioException error, String fallback) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return data['message'] as String;
    }
    return fallback;
  }
}

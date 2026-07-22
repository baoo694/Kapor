import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

class TechTalkScenario {
  const TechTalkScenario({
    required this.id,
    required this.title,
    required this.titleVi,
    required this.domain,
    required this.difficulty,
    required this.missionVi,
    required this.persona,
    required this.requiredVocabulary,
  });
  final String id, title, titleVi, domain, difficulty, missionVi;
  final TechTalkPersona persona;
  final List<String> requiredVocabulary;
  factory TechTalkScenario.fromJson(Map<String, dynamic> json) =>
      TechTalkScenario(
        id: json['id']?.toString() ?? '',
        title: json['title']?.toString() ?? '',
        titleVi: json['titleVi']?.toString() ?? '',
        domain: json['domain']?.toString() ?? '',
        difficulty: json['difficulty']?.toString() ?? '',
        missionVi: json['missionVi']?.toString() ?? '',
        persona: TechTalkPersona.fromJson(
          json['persona'] is Map
              ? Map<String, dynamic>.from(json['persona'] as Map)
              : const {},
        ),
        requiredVocabulary: json['requiredVocabulary'] is List
            ? (json['requiredVocabulary'] as List)
                  .map((item) => item.toString())
                  .toList()
            : const [],
      );
}

class TechTalkPersona {
  const TechTalkPersona({
    required this.name,
    required this.role,
    required this.company,
    required this.avatar,
  });
  final String name, role, company, avatar;
  factory TechTalkPersona.fromJson(Map<String, dynamic> json) =>
      TechTalkPersona(
        name: json['name']?.toString() ?? 'AI Tech Lead',
        role: json['role']?.toString() ?? 'Tech Lead',
        company: json['company']?.toString() ?? '',
        avatar: json['avatar']?.toString() ?? '🤖',
      );
}

class MessageEvaluation {
  const MessageEvaluation({
    required this.grammar,
    required this.vocabulary,
    required this.politeness,
    required this.corrections,
  });
  final int grammar, vocabulary, politeness;
  final List<String> corrections;
  factory MessageEvaluation.fromJson(Map<String, dynamic> json) =>
      MessageEvaluation(
        grammar: (json['grammar'] as num?)?.toInt() ?? 0,
        vocabulary: (json['vocabulary'] as num?)?.toInt() ?? 0,
        politeness: (json['politeness'] as num?)?.toInt() ?? 0,
        corrections: json['corrections'] is List
            ? (json['corrections'] as List)
                  .map((item) => item.toString())
                  .toList()
            : const [],
      );
}

class RoleplayMessage {
  const RoleplayMessage({
    required this.id,
    required this.role,
    required this.content,
    this.evaluation,
  });
  final String id, role, content;
  final MessageEvaluation? evaluation;
  factory RoleplayMessage.fromJson(Map<String, dynamic> json) =>
      RoleplayMessage(
        id: json['id']?.toString() ?? '',
        role: json['role']?.toString() ?? 'ai',
        content: json['content']?.toString() ?? '',
        evaluation: json['evaluation'] is Map
            ? MessageEvaluation.fromJson(
                Map<String, dynamic>.from(json['evaluation'] as Map),
              )
            : null,
      );
}

class RoleplaySession {
  const RoleplaySession({
    required this.id,
    required this.scenarioId,
    required this.status,
    required this.messages,
    this.finalEvaluation,
  });
  final String id, scenarioId, status;
  final List<RoleplayMessage> messages;
  final RoleplayFinalEvaluation? finalEvaluation;
  factory RoleplaySession.fromJson(Map<String, dynamic> json) =>
      RoleplaySession(
        id: json['id']?.toString() ?? '',
        scenarioId: json['scenarioId']?.toString() ?? '',
        status: json['status']?.toString() ?? 'active',
        messages: json['messages'] is List
            ? (json['messages'] as List)
                  .whereType<Map>()
                  .map(
                    (item) => RoleplayMessage.fromJson(
                      Map<String, dynamic>.from(item),
                    ),
                  )
                  .toList()
            : const [],
        finalEvaluation: json['finalEvaluation'] is Map
            ? RoleplayFinalEvaluation.fromJson(
                Map<String, dynamic>.from(json['finalEvaluation'] as Map),
              )
            : null,
      );
}

class RoleplayFinalEvaluation {
  const RoleplayFinalEvaluation({
    required this.overallScore,
    required this.grammar,
    required this.vocabulary,
    required this.politeness,
    required this.taskCompletion,
    required this.feedbackVi,
  });
  final int overallScore, grammar, vocabulary, politeness, taskCompletion;
  final String feedbackVi;
  factory RoleplayFinalEvaluation.fromJson(Map<String, dynamic> json) =>
      RoleplayFinalEvaluation(
        overallScore: (json['overallScore'] as num?)?.toInt() ?? 0,
        grammar: (json['grammar'] as num?)?.toInt() ?? 0,
        vocabulary: (json['vocabulary'] as num?)?.toInt() ?? 0,
        politeness: (json['politeness'] as num?)?.toInt() ?? 0,
        taskCompletion: (json['taskCompletion'] as num?)?.toInt() ?? 0,
        feedbackVi: json['feedbackVi']?.toString() ?? '',
      );
}

class RoleplayHint {
  const RoleplayHint({
    required this.keywords,
    required this.sentenceStructure,
    required this.politenessTip,
  });
  final List<String> keywords;
  final String sentenceStructure, politenessTip;
  factory RoleplayHint.fromJson(Map<String, dynamic> json) => RoleplayHint(
    keywords: json['keywords'] is List
        ? (json['keywords'] as List).map((item) => item.toString()).toList()
        : const [],
    sentenceStructure: json['sentenceStructure']?.toString() ?? '',
    politenessTip: json['politenessTip']?.toString() ?? '',
  );
}

class TechTalkService {
  TechTalkService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;
  Future<List<TechTalkScenario>> scenarios() async =>
      _list('/scenarios', TechTalkScenario.fromJson);
  Future<RoleplaySession> start(String scenarioId) => _session(
    _dio.post('/roleplay/start', queryParameters: {'scenarioId': scenarioId}),
  );
  Future<RoleplaySession> send(String sessionId, String content) => _session(
    _dio.post('/roleplay/$sessionId/send', data: {'content': content}),
  );
  Future<RoleplaySession> end(String sessionId) =>
      _session(_dio.post('/roleplay/$sessionId/end'));
  Future<RoleplayHint> hint(String sessionId) async {
    final data = await _data(_dio.post('/roleplay/$sessionId/hint'));
    return RoleplayHint.fromJson(Map<String, dynamic>.from(data as Map));
  }

  Future<RoleplaySession> _session(Future<Response<dynamic>> response) async =>
      RoleplaySession.fromJson(
        Map<String, dynamic>.from((await _data(response)) as Map),
      );
  Future<List<T>> _list<T>(
    String path,
    T Function(Map<String, dynamic>) fromJson,
  ) async {
    final data = await _data(_dio.get(path));
    if (data is! List) throw const FormatException('Danh sách không hợp lệ.');
    return data
        .whereType<Map>()
        .map((item) => fromJson(Map<String, dynamic>.from(item)))
        .toList();
  }

  Future<dynamic> _data(Future<Response<dynamic>> response) async {
    try {
      final body = (await response).data;
      if (body is! Map || body['success'] != true)
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể xử lý TechTalk.'
              : 'Phản hồi không hợp lệ.',
        );
      return body['data'];
    } on DioException catch (error) {
      final data = error.response?.data;
      throw Exception(
        data is Map && data['message'] is String
            ? data['message']
            : 'Không thể kết nối TechTalk.',
      );
    }
  }
}

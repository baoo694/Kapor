import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

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
    required this.objectives,
    required this.mission,
    required this.order,
  });

  final String id, title, titleVi, domain, difficulty, missionVi;
  final TechTalkPersona persona;
  final List<String> requiredVocabulary;
  final List<String> objectives;
  final TechTalkMission mission;
  final int order;

  factory TechTalkScenario.fromJson(Map<String, dynamic> json) {
    final missionJson = _map(json['mission']);
    final legacyVocabulary = _strings(json['requiredVocabulary']);
    return TechTalkScenario(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      titleVi: json['titleVi']?.toString() ?? '',
      domain: json['domain']?.toString() ?? '',
      difficulty: json['difficulty']?.toString() ?? '',
      missionVi:
          json['missionVi']?.toString() ??
          missionJson['titleVi']?.toString() ??
          '',
      persona: TechTalkPersona.fromJson(_map(json['persona'])),
      requiredVocabulary: _strings(missionJson['requiredVocabulary']).isNotEmpty
          ? _strings(missionJson['requiredVocabulary'])
          : legacyVocabulary,
      objectives: _strings(json['objectives']),
      mission: TechTalkMission.fromJson(missionJson),
      order: (json['order'] as num?)?.toInt() ?? 0,
    );
  }
}

class TechTalkMission {
  const TechTalkMission({
    required this.titleKo,
    required this.titleVi,
    required this.contextPrompt,
    required this.objectives,
  });

  final String titleKo, titleVi, contextPrompt;
  final List<TechTalkObjective> objectives;

  factory TechTalkMission.fromJson(
    Map<String, dynamic> json,
  ) => TechTalkMission(
    titleKo: json['titleKo']?.toString() ?? '',
    titleVi: json['titleVi']?.toString() ?? '',
    contextPrompt: json['contextPrompt']?.toString() ?? '',
    objectives: json['objectives'] is List
        ? (json['objectives'] as List)
              .whereType<Map>()
              .map(
                (item) =>
                    TechTalkObjective.fromJson(Map<String, dynamic>.from(item)),
              )
              .toList()
        : const [],
  );
}

class TechTalkObjective {
  const TechTalkObjective({
    required this.ko,
    required this.vi,
    required this.en,
  });
  final String ko, vi, en;
  factory TechTalkObjective.fromJson(Map<String, dynamic> json) =>
      TechTalkObjective(
        ko: json['ko']?.toString() ?? '',
        vi: json['vi']?.toString() ?? '',
        en: json['en']?.toString() ?? '',
      );
}

class TechTalkPersona {
  const TechTalkPersona({
    required this.name,
    required this.role,
    required this.company,
    required this.avatar,
    required this.avatarUrl,
    required this.speechStyle,
    required this.personality,
  });

  final String name, role, company, avatar, avatarUrl, speechStyle, personality;

  factory TechTalkPersona.fromJson(Map<String, dynamic> json) =>
      TechTalkPersona(
        name: json['name']?.toString() ?? 'AI Tech Lead',
        role: json['role']?.toString() ?? 'Tech Lead',
        company: json['company']?.toString() ?? '',
        avatar: json['avatar']?.toString() ?? '🤖',
        avatarUrl: json['avatarUrl']?.toString() ?? '',
        speechStyle: json['speechStyle']?.toString() ?? 'hasipsio',
        personality: json['personality']?.toString() ?? '',
      );
}

class MessageCorrection {
  const MessageCorrection({
    required this.original,
    required this.suggestion,
    required this.type,
    required this.noteVi,
  });
  final String original, suggestion, type, noteVi;
  factory MessageCorrection.fromJson(Map<String, dynamic> json) =>
      MessageCorrection(
        original: json['original']?.toString() ?? '',
        suggestion:
            json['suggestion']?.toString() ??
            json['corrected']?.toString() ??
            '',
        type: json['type']?.toString() ?? 'grammar',
        noteVi:
            json['noteVi']?.toString() ??
            json['note']?.toString() ??
            json['explanation']?.toString() ??
            '',
      );
}

class MessageEvaluation {
  const MessageEvaluation({
    required this.grammar,
    required this.vocabulary,
    required this.politeness,
    required this.corrections,
    required this.status,
    required this.feedbackVi,
    required this.usedRequiredVocabulary,
    required this.objectives,
    required this.allObjectivesCompleted,
    required this.completionMessageKo,
    this.errorCode,
  });

  final int grammar, vocabulary, politeness;
  final List<MessageCorrection> corrections;
  final String status, feedbackVi;
  final List<String> usedRequiredVocabulary;
  final List<ObjectiveResult> objectives;
  final bool allObjectivesCompleted;
  final String completionMessageKo;
  final String? errorCode;

  factory MessageEvaluation.fromJson(Map<String, dynamic> json) =>
      MessageEvaluation(
        grammar: (json['grammar'] as num?)?.toInt() ?? 0,
        vocabulary: (json['vocabulary'] as num?)?.toInt() ?? 0,
        politeness: (json['politeness'] as num?)?.toInt() ?? 0,
        status: json['status']?.toString() ?? 'completed',
        feedbackVi: json['feedbackVi']?.toString() ?? '',
        usedRequiredVocabulary: _strings(json['usedRequiredVocabulary']),
        objectives: json['objectives'] is List
            ? (json['objectives'] as List)
                  .whereType<Map>()
                  .map((item) => ObjectiveResult.fromJson(_map(item)))
                  .toList()
            : const [],
        allObjectivesCompleted: json['allObjectivesCompleted'] == true,
        completionMessageKo: json['completionMessageKo']?.toString() ?? '',
        errorCode: json['errorCode']?.toString(),
        corrections: json['corrections'] is List
            ? (json['corrections'] as List)
                  .map(
                    (item) => item is Map
                        ? MessageCorrection.fromJson(
                            Map<String, dynamic>.from(item),
                          )
                        : MessageCorrection(
                            original: '',
                            suggestion: '',
                            type: 'grammar',
                            noteVi: item.toString(),
                          ),
                  )
                  .toList()
            : const [],
      );
}

class RoleplayMessage {
  const RoleplayMessage({
    required this.id,
    required this.role,
    required this.content,
    required this.source,
    required this.generationStatus,
    this.clientTurnId,
    this.transcript,
    this.audioId,
    this.timestamp,
    this.evaluation,
  });

  final String id, role, content, source, generationStatus;
  final String? clientTurnId, transcript, audioId;
  final DateTime? timestamp;
  final MessageEvaluation? evaluation;

  factory RoleplayMessage.fromJson(Map<String, dynamic> json) =>
      RoleplayMessage(
        id: json['id']?.toString() ?? '',
        role: json['role']?.toString() ?? 'ai',
        content: json['content']?.toString() ?? '',
        source: json['source']?.toString() ?? 'text',
        generationStatus: json['generationStatus']?.toString() ?? 'completed',
        clientTurnId: json['clientTurnId']?.toString(),
        transcript: json['transcript']?.toString(),
        audioId: json['audioId']?.toString(),
        timestamp: DateTime.tryParse(json['timestamp']?.toString() ?? ''),
        evaluation: json['evaluation'] is Map
            ? MessageEvaluation.fromJson(_map(json['evaluation']))
            : null,
      );

  RoleplayMessage copyWith({
    String? id,
    String? content,
    String? generationStatus,
    MessageEvaluation? evaluation,
  }) => RoleplayMessage(
    id: id ?? this.id,
    role: role,
    content: content ?? this.content,
    source: source,
    generationStatus: generationStatus ?? this.generationStatus,
    clientTurnId: clientTurnId,
    transcript: transcript,
    audioId: audioId,
    timestamp: timestamp,
    evaluation: evaluation ?? this.evaluation,
  );
}

class RoleplaySession {
  const RoleplaySession({
    required this.id,
    required this.scenarioId,
    required this.status,
    required this.messages,
    required this.hintsUsed,
    required this.durationSeconds,
    required this.objectiveProgress,
    this.objectivesCompletedAt,
    this.finalEvaluation,
  });

  final String id, scenarioId, status;
  final List<RoleplayMessage> messages;
  final int hintsUsed;
  final int durationSeconds;
  final List<ObjectiveResult> objectiveProgress;
  final DateTime? objectivesCompletedAt;
  final RoleplayFinalEvaluation? finalEvaluation;

  factory RoleplaySession.fromJson(Map<String, dynamic> json) =>
      RoleplaySession(
        id: json['id']?.toString() ?? '',
        scenarioId: json['scenarioId']?.toString() ?? '',
        status: json['status']?.toString() ?? 'active',
        messages: json['messages'] is List
            ? (json['messages'] as List)
                  .whereType<Map>()
                  .map((item) => RoleplayMessage.fromJson(_map(item)))
                  .toList()
            : const [],
        hintsUsed: (json['hintsUsed'] as num?)?.toInt() ?? 0,
        durationSeconds: (json['durationSeconds'] as num?)?.toInt() ?? 0,
        objectiveProgress: json['objectiveProgress'] is List
            ? (json['objectiveProgress'] as List)
                  .whereType<Map>()
                  .map((item) => ObjectiveResult.fromJson(_map(item)))
                  .toList()
            : const [],
        objectivesCompletedAt: DateTime.tryParse(
          json['objectivesCompletedAt']?.toString() ?? '',
        ),
        finalEvaluation: json['finalEvaluation'] is Map
            ? RoleplayFinalEvaluation.fromJson(_map(json['finalEvaluation']))
            : null,
      );

  RoleplaySession copyWith({
    List<RoleplayMessage>? messages,
    String? status,
    RoleplayFinalEvaluation? finalEvaluation,
    List<ObjectiveResult>? objectiveProgress,
    DateTime? objectivesCompletedAt,
  }) => RoleplaySession(
    id: id,
    scenarioId: scenarioId,
    status: status ?? this.status,
    messages: messages ?? this.messages,
    hintsUsed: hintsUsed,
    durationSeconds: durationSeconds,
    objectiveProgress: objectiveProgress ?? this.objectiveProgress,
    objectivesCompletedAt: objectivesCompletedAt ?? this.objectivesCompletedAt,
    finalEvaluation: finalEvaluation ?? this.finalEvaluation,
  );
}

class ObjectiveResult {
  const ObjectiveResult({
    required this.objective,
    required this.completed,
    required this.evidence,
  });
  final String objective, evidence;
  final bool completed;
  factory ObjectiveResult.fromJson(Map<String, dynamic> json) =>
      ObjectiveResult(
        objective: json['objective']?.toString() ?? '',
        completed: json['completed'] == true,
        evidence: json['evidence']?.toString() ?? '',
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
    required this.feedback,
    required this.improvementAreas,
    required this.objectives,
    this.evaluationErrorCode,
  });

  final int overallScore, grammar, vocabulary, politeness, taskCompletion;
  final String feedbackVi, feedback;
  final List<String> improvementAreas;
  final List<ObjectiveResult> objectives;
  final String? evaluationErrorCode;

  factory RoleplayFinalEvaluation.fromJson(Map<String, dynamic> json) =>
      RoleplayFinalEvaluation(
        overallScore: (json['overallScore'] as num?)?.toInt() ?? 0,
        grammar: (json['grammar'] as num?)?.toInt() ?? 0,
        vocabulary: (json['vocabulary'] as num?)?.toInt() ?? 0,
        politeness: (json['politeness'] as num?)?.toInt() ?? 0,
        taskCompletion: (json['taskCompletion'] as num?)?.toInt() ?? 0,
        feedbackVi: json['feedbackVi']?.toString() ?? '',
        feedback: json['feedback']?.toString() ?? '',
        improvementAreas: _strings(json['improvementAreas']),
        objectives: json['objectives'] is List
            ? (json['objectives'] as List)
                  .whereType<Map>()
                  .map((item) => ObjectiveResult.fromJson(_map(item)))
                  .toList()
            : const [],
        evaluationErrorCode: json['evaluationErrorCode']?.toString(),
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
    keywords: _strings(json['keywords']),
    sentenceStructure: json['sentenceStructure']?.toString() ?? '',
    politenessTip: json['politenessTip']?.toString() ?? '',
  );
}

class RoleplayTranscription {
  const RoleplayTranscription({
    required this.audioId,
    required this.transcript,
    required this.confidence,
    required this.durationMs,
  });
  final String audioId, transcript;
  final double? confidence;
  final int durationMs;
  factory RoleplayTranscription.fromJson(Map<String, dynamic> json) =>
      RoleplayTranscription(
        audioId: json['audioId']?.toString() ?? '',
        transcript: json['transcript']?.toString() ?? '',
        confidence: (json['confidence'] as num?)?.toDouble(),
        durationMs: (json['durationMs'] as num?)?.toInt() ?? 0,
      );
}

class RoleplayStreamEvent {
  const RoleplayStreamEvent({
    required this.type,
    this.turnId,
    this.userMessageId,
    this.messageId,
    this.delta,
    this.message,
    this.evaluation,
    this.code,
    this.messageText,
    this.retryable = false,
    this.allObjectivesCompleted = false,
  });

  final String type;
  final String? turnId, userMessageId, messageId, delta, code, messageText;
  final RoleplayMessage? message;
  final MessageEvaluation? evaluation;
  final bool retryable;
  final bool allObjectivesCompleted;

  factory RoleplayStreamEvent.fromJson(
    String eventType,
    Map<String, dynamic> json,
  ) => RoleplayStreamEvent(
    type: json['type']?.toString() ?? eventType,
    turnId: json['turnId']?.toString(),
    userMessageId: json['userMessageId']?.toString(),
    messageId: json['messageId']?.toString(),
    delta: json['delta']?.toString(),
    code: json['code']?.toString(),
    messageText: json['messageText']?.toString(),
    retryable: json['retryable'] == true,
    allObjectivesCompleted: json['allObjectivesCompleted'] == true,
    message: json['message'] is Map
        ? RoleplayMessage.fromJson(_map(json['message']))
        : null,
    evaluation: json['evaluation'] is Map
        ? MessageEvaluation.fromJson(_map(json['evaluation']))
        : null,
  );
}

class RoleplayHistoryPage {
  const RoleplayHistoryPage({
    required this.content,
    required this.page,
    required this.size,
    required this.hasMore,
  });
  final List<RoleplaySession> content;
  final int page, size;
  final bool hasMore;
  factory RoleplayHistoryPage.fromJson(Map<String, dynamic> json) =>
      RoleplayHistoryPage(
        content: json['content'] is List
            ? (json['content'] as List)
                  .whereType<Map>()
                  .map((item) => RoleplaySession.fromJson(_map(item)))
                  .toList()
            : const [],
        page: (json['page'] as num?)?.toInt() ?? 0,
        size: (json['size'] as num?)?.toInt() ?? 20,
        hasMore: json['hasMore'] == true,
      );
}

class TechTalkService {
  TechTalkService({Dio? dio}) : _dio = dio ?? ApiClient().dio;
  final Dio _dio;

  Future<List<TechTalkScenario>> scenarios() async =>
      _list('/scenarios', TechTalkScenario.fromJson);

  Future<RoleplaySession> start(String scenarioId, {bool testMode = false}) =>
      _session(
        _dio.post(
          '/roleplay/start',
          data: {'scenarioId': scenarioId, 'testMode': testMode},
        ),
      );

  Future<RoleplaySession> send(String sessionId, String content) => _session(
    _dio.post('/roleplay/$sessionId/send', data: {'content': content}),
  );

  Stream<RoleplayStreamEvent> streamTurn({
    required String sessionId,
    required String clientTurnId,
    required String content,
    String source = 'text',
    String? transcript,
    String? audioId,
  }) async* {
    try {
      final response = await _dio.post<ResponseBody>(
        '/roleplay/$sessionId/turns/stream',
        data: {
          'clientTurnId': clientTurnId,
          'content': content,
          'source': source,
          'transcript': ?transcript,
          'audioId': ?audioId,
        },
        options: Options(
          responseType: ResponseType.stream,
          headers: {'Accept': 'text/event-stream'},
          receiveTimeout: const Duration(minutes: 2),
        ),
      );
      final body = response.data;
      if (body == null) {
        throw const FormatException('TechTalk stream is empty.');
      }
      var eventName = 'message';
      final dataLines = <String>[];
      await for (final line
          in body.stream
              .cast<List<int>>()
              .transform(utf8.decoder)
              .transform(const LineSplitter())) {
        if (line.isEmpty) {
          if (dataLines.isNotEmpty) {
            final decoded = jsonDecode(dataLines.join('\n'));
            if (decoded is Map) {
              yield RoleplayStreamEvent.fromJson(
                eventName,
                Map<String, dynamic>.from(decoded),
              );
            }
          }
          eventName = 'message';
          dataLines.clear();
        } else if (line.startsWith('event:')) {
          eventName = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          dataLines.add(line.substring(5).trimLeft());
        }
      }
      if (dataLines.isNotEmpty) {
        final decoded = jsonDecode(dataLines.join('\n'));
        if (decoded is Map) {
          yield RoleplayStreamEvent.fromJson(
            eventName,
            Map<String, dynamic>.from(decoded),
          );
        }
      }
    } on DioException catch (error) {
      throw Exception(
        _errorMessage(error, 'Không thể kết nối luồng TechTalk.'),
      );
    } on FormatException catch (error) {
      throw Exception(error.message);
    }
  }

  Future<RoleplaySession> end(String sessionId) =>
      _session(_dio.post('/roleplay/$sessionId/end'));

  Future<RoleplaySession> abandon(String sessionId) =>
      _session(_dio.post('/roleplay/$sessionId/abandon'));

  Future<RoleplaySession> detail(String sessionId) =>
      _session(_dio.get('/roleplay/$sessionId'));

  Future<RoleplayHint> hint(String sessionId) async {
    final data = await _data(_dio.post('/roleplay/$sessionId/hint'));
    return RoleplayHint.fromJson(_map(data));
  }

  Future<RoleplayHistoryPage> history({int page = 0, int size = 20}) async {
    final data = await _data(
      _dio.get(
        '/roleplay/history/page',
        queryParameters: {'page': page, 'size': size},
      ),
    );
    return RoleplayHistoryPage.fromJson(_map(data));
  }

  Future<RoleplayTranscription> transcribe({
    required String sessionId,
    required Uint8List bytes,
  }) async {
    final form = FormData.fromMap({
      'audioFile': MultipartFile.fromBytes(
        bytes,
        filename: 'techtalk.pcm',
        contentType: MediaType('audio', 'pcm'),
      ),
    });
    final data = await _data(
      _dio.post(
        '/roleplay/$sessionId/audio/transcribe',
        data: form,
        options: Options(contentType: 'multipart/form-data'),
      ),
    );
    return RoleplayTranscription.fromJson(_map(data));
  }

  Future<RoleplaySession> _session(Future<Response<dynamic>> response) async =>
      RoleplaySession.fromJson(_map(await _data(response)));

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
      if (body is! Map || body['success'] != true) {
        throw Exception(
          body is Map
              ? body['message'] ?? 'Không thể xử lý TechTalk.'
              : 'Phản hồi không hợp lệ.',
        );
      }
      return body['data'];
    } on DioException catch (error) {
      throw Exception(_errorMessage(error, 'Không thể kết nối TechTalk.'));
    }
  }

  String _errorMessage(DioException error, String fallback) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return data['message'] as String;
    }
    return fallback;
  }
}

Map<String, dynamic> _map(dynamic value) =>
    value is Map ? Map<String, dynamic>.from(value) : <String, dynamic>{};

List<String> _strings(dynamic value) =>
    value is List ? value.map((item) => item.toString()).toList() : const [];

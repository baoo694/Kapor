import 'package:flutter_test/flutter_test.dart';
import 'package:kapor_flutter/features/techtalk/data/techtalk_service.dart';
import 'package:kapor_flutter/features/techtalk/techtalk_strings.dart';

void main() {
  test(
    'parses the versioned TechTalk scenario and prefers mission vocabulary',
    () {
      final scenario = TechTalkScenario.fromJson({
        'id': 'incident',
        'title': '서버 장애 보고',
        'titleVi': 'Báo cáo sự cố',
        'domain': 'backend',
        'difficulty': 'intermediate',
        'order': 10,
        'persona': {
          'name': '김민수',
          'role': 'Tech Lead',
          'company': 'Kapor',
          'speechStyle': 'hasipsio',
        },
        'requiredVocabulary': ['legacy'],
        'mission': {
          'titleKo': '프로덕션 장애 보고',
          'titleVi': 'Báo cáo lỗi production',
          'contextPrompt': 'A production API is unavailable.',
          'requiredVocabulary': ['장애', '롤백'],
          'objectives': [
            {'ko': '영향을 설명합니다.', 'vi': 'Giải thích ảnh hưởng.'},
          ],
        },
      });

      expect(scenario.persona.name, '김민수');
      expect(scenario.mission.objectives.single.ko, '영향을 설명합니다.');
      expect(scenario.requiredVocabulary, ['장애', '롤백']);
      expect(scenario.order, 10);
    },
  );

  test('parses structured and legacy per-turn corrections safely', () {
    final session = RoleplaySession.fromJson({
      'id': 'session-1',
      'scenarioId': 'incident',
      'status': 'active',
      'messages': [
        {
          'id': 'message-1',
          'role': 'user',
          'content': '서버가 문제 있어요.',
          'evaluation': {
            'grammar': 70,
            'vocabulary': 80,
            'politeness': 65,
            'status': 'completed',
            'corrections': [
              {
                'original': '문제 있어요',
                'suggestion': '문제가 있습니다',
                'type': 'formality',
                'noteVi': 'Dùng đuôi câu trang trọng.',
              },
              'legacy correction',
            ],
          },
        },
      ],
    });

    final evaluation = session.messages.single.evaluation!;
    expect(evaluation.grammar, 70);
    expect(evaluation.corrections, hasLength(2));
    expect(evaluation.corrections.first.suggestion, '문제가 있습니다');
    expect(evaluation.corrections.last.noteVi, 'legacy correction');
  });

  test('parses SSE message completion and bilingual labels', () {
    final event = RoleplayStreamEvent.fromJson('message.completed', {
      'turnId': 'turn-1',
      'message': {
        'id': 'ai-1',
        'role': 'ai',
        'content': '영향 범위를 설명해 주세요.',
        'source': 'ai',
        'generationStatus': 'completed',
      },
    });

    expect(event.type, 'message.completed');
    expect(event.message?.content, '영향 범위를 설명해 주세요.');
    expect(const TechTalkStrings('vi').history, 'Lịch sử');
    expect(const TechTalkStrings('en').history, 'History');
  });

  test('parses per-turn objective progress and mission completion event', () {
    final session = RoleplaySession.fromJson({
      'id': 'session-1',
      'scenarioId': 'incident',
      'status': 'active',
      'objectiveProgress': [
        {
          'objective': '사용자 영향을 설명합니다.',
          'completed': true,
          'evidence': '일부 사용자에게 영향이 있습니다.',
        },
      ],
      'objectivesCompletedAt': '2026-08-01T10:00:00Z',
      'messages': [
        {
          'id': 'user-1',
          'role': 'user',
          'content': '일부 사용자에게 영향이 있습니다.',
          'evaluation': {
            'grammar': 90,
            'vocabulary': 90,
            'politeness': 90,
            'status': 'completed',
            'objectives': [
              {
                'objective': '사용자 영향을 설명합니다.',
                'completed': true,
                'evidence': '일부 사용자에게 영향이 있습니다.',
              },
            ],
            'allObjectivesCompleted': true,
            'completionMessageKo': '필요한 내용을 모두 확인했습니다.',
          },
        },
      ],
    });
    final event = RoleplayStreamEvent.fromJson('mission.completed', {
      'allObjectivesCompleted': true,
    });

    expect(session.objectiveProgress.single.completed, isTrue);
    expect(session.objectivesCompletedAt, isNotNull);
    expect(session.messages.single.evaluation!.allObjectivesCompleted, isTrue);
    expect(event.type, 'mission.completed');
    expect(event.allObjectivesCompleted, isTrue);
    expect(
      const TechTalkStrings('vi').allObjectivesCompletedMessage,
      'Bạn đã hoàn thành tất cả mục tiêu.',
    );
  });
}

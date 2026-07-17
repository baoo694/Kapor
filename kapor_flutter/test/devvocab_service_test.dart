import 'package:flutter_test/flutter_test.dart';
import 'package:kapor_flutter/features/devvocab/data/devvocab_service.dart';

void main() {
  test('parses a lesson and its vocabulary from the backend response', () {
    final lesson = DevVocabLesson.fromJson({
      'id': 'lesson-1',
      'topicId': 'topic-1',
      'title': 'Flexbox 방향 속성',
      'titleVi': 'Thuộc tính hướng Flexbox',
      'content': '한국어 내용',
      'contentVi': 'Nội dung tiếng Việt',
      'order': 1,
      'vocabulary': [
        {
          'id': 'word-1',
          'korean': '방향',
          'pronunciation': 'banghyang',
          'vietnamese': 'Hướng',
          'english': 'Direction',
          'context': 'CSS flex-direction',
          'codeSnippet': 'flex-direction: row;',
          'audioUrl': 'https://example.com/audio.mp3',
        },
      ],
    });

    expect(lesson.id, 'lesson-1');
    expect(lesson.topicId, 'topic-1');
    expect(lesson.order, 1);
    expect(lesson.vocabulary, hasLength(1));
    expect(lesson.vocabulary.single.korean, '방향');
    expect(lesson.vocabulary.single.codeSnippet, 'flex-direction: row;');
  });

  test('parses persisted known and learning card counts', () {
    final progress = FlashcardProgress.fromJson({
      'lessonId': 'lesson-1',
      'totalCards': 3,
      'knownCards': 1,
      'learningCards': 2,
      'cardStatuses': {'word-1': 'KNOWN', 'word-2': 'LEARNING'},
    });

    expect(progress.totalCards, 3);
    expect(progress.knownCards, 1);
    expect(progress.learningCards, 2);
    expect(progress.cardStatuses['word-1'], 'KNOWN');
  });
}

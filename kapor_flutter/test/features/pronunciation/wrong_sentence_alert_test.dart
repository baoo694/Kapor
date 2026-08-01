import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kapor_flutter/features/pronunciation/widgets/wrong_sentence_alert.dart';

void main() {
  testWidgets('shows WhisperX evidence and lets the learner retry', (
    tester,
  ) async {
    var retried = false;
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.dark(),
        home: Scaffold(
          body: WrongSentenceAlert(
            expectedText: '비동기 처리를 구현했습니다',
            transcriptText: '네 안녕하세요 저는 파로고입니다',
            message: 'Nội dung khác câu mẫu. Hãy đọc lại.',
            onRetry: () => retried = true,
          ),
        ),
      ),
    );

    expect(find.text('Bạn đã đọc khác câu mẫu'), findsOneWidget);
    expect(find.text('비동기 처리를 구현했습니다'), findsOneWidget);
    expect(find.text('네 안녕하세요 저는 파로고입니다'), findsOneWidget);
    expect(
      find.text('Lần đọc này không được tính điểm phát âm.'),
      findsOneWidget,
    );

    await tester.tap(find.text('Ghi âm và đọc lại'));
    expect(retried, isTrue);
  });
}

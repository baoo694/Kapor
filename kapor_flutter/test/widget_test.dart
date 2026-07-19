import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:kapor_flutter/main.dart';

void main() {
  test('exposes the Kapor application root widget', () {
    expect(const KaporApp(), isA<StatelessWidget>());
  });
}

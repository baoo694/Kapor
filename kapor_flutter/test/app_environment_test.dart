import 'package:flutter_test/flutter_test.dart';
import 'package:kapor_flutter/core/config/app_environment.dart';
import 'package:kapor_flutter/core/network/api_client.dart';

void main() {
  testWidgets('loads the API base URL from the environment asset', (_) async {
    await AppEnvironment.load();

    expect(AppEnvironment.apiBaseUrl, isNotEmpty);
    expect(ApiClient().dio.options.baseUrl, AppEnvironment.apiBaseUrl);
  });
}

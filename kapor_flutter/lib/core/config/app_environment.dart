import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Environment values loaded from the root `.env` asset at application startup.
class AppEnvironment {
  AppEnvironment._();

  static Future<void> load() => dotenv.load(fileName: '.env');

  static String get apiBaseUrl {
    final value = dotenv.env['API_BASE_URL']?.trim();
    if (value == null || value.isEmpty) {
      throw StateError(
        'API_BASE_URL is missing. Set it in the root .env file before running the app.',
      );
    }

    return value.endsWith('/') ? value.substring(0, value.length - 1) : value;
  }
}

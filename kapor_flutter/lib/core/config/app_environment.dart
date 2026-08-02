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

  /// OAuth 2.0 Web client ID. This is an identifier, not a secret, and is
  /// supplied to the Google native SDK so it issues an ID token for the API.
  static String get googleServerClientId {
    final value = dotenv.env['GOOGLE_SERVER_CLIENT_ID']?.trim();
    if (value == null || value.isEmpty) {
      throw StateError(
        'GOOGLE_SERVER_CLIENT_ID is missing. Add the Web OAuth client ID to .env before using Google sign-in.',
      );
    }
    if (!value.endsWith('.apps.googleusercontent.com')) {
      throw StateError(
        'GOOGLE_SERVER_CLIENT_ID không phải Google OAuth Web client ID hợp lệ.',
      );
    }

    return value;
  }
}

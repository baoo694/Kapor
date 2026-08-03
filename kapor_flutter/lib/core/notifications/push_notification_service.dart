import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import '../network/api_client.dart';

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // The native Firebase configuration must be present before background
  // delivery can run. Initialization failures are intentionally non-fatal.
  try {
    await Firebase.initializeApp();
  } catch (_) {}
}

/// Registers the current install with Kapor's backend for daily-goal reminders.
/// Firebase stays optional so development builds without Firebase files continue
/// to work normally.
class PushNotificationService {
  PushNotificationService._();
  static final instance = PushNotificationService._();

  final Dio _dio = ApiClient().dio;
  StreamSubscription<String>? _tokenRefreshSubscription;
  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized || kIsWeb || !(Platform.isAndroid || Platform.isIOS)) {
      return;
    }
    try {
      await Firebase.initializeApp();
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
      _initialized = true;
      _tokenRefreshSubscription = FirebaseMessaging.instance.onTokenRefresh
          .listen(
            _registerToken,
            onError: (Object error, StackTrace stackTrace) {
              debugPrint('FCM token refresh failed: $error');
            },
          );
    } catch (error) {
      debugPrint('Firebase is not configured for this build: $error');
    }
  }

  Future<void> registerCurrentDevice() async {
    if (!_initialized) return;
    try {
      final settings = await FirebaseMessaging.instance.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      if (settings.authorizationStatus == AuthorizationStatus.denied) return;
      final token = await FirebaseMessaging.instance.getToken();
      if (token != null && token.isNotEmpty) await _registerToken(token);
    } catch (error) {
      debugPrint('Could not register this device for notifications: $error');
    }
  }

  Future<void> _registerToken(String token) async {
    try {
      await _dio.post(
        '/notifications/devices',
        data: {
          'token': token,
          'platform': Platform.isIOS ? 'ios' : 'android',
          'timezoneOffsetMinutes': DateTime.now().timeZoneOffset.inMinutes,
        },
      );
    } on DioException catch (error) {
      debugPrint('Could not sync FCM token: ${error.message}');
    }
  }

  Future<void> dispose() async {
    await _tokenRefreshSubscription?.cancel();
  }
}

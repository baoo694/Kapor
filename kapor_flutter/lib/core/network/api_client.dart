import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_environment.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  late Dio dio;
  Future<bool>? _refreshInFlight;

  static String get baseUrl => AppEnvironment.apiBaseUrl;

  factory ApiClient() {
    return _instance;
  }

  ApiClient._internal() {
    dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 15),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          options.headers['X-Timezone-Offset-Minutes'] = DateTime.now()
              .timeZoneOffset
              .inMinutes
              .toString();
          if (options.extra['skipAuth'] == true ||
              options.path.startsWith('/auth/')) {
            return handler.next(options);
          }

          final prefs = await SharedPreferences.getInstance();
          final token = prefs.getString('access_token');
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onError: (DioException error, handler) async {
          final request = error.requestOptions;
          final canRetry =
              error.response?.statusCode == 401 &&
              request.extra['skipAuth'] != true &&
              !request.path.startsWith('/auth/') &&
              request.extra['retriedAfterRefresh'] != true;

          if (!canRetry || !await _refreshAccessToken()) {
            return handler.next(error);
          }

          try {
            final prefs = await SharedPreferences.getInstance();
            final accessToken = prefs.getString('access_token');
            if (accessToken == null || accessToken.isEmpty) {
              return handler.next(error);
            }

            request.headers['Authorization'] = 'Bearer $accessToken';
            request.extra['retriedAfterRefresh'] = true;
            final response = await dio.fetch<dynamic>(request);
            return handler.resolve(response);
          } on DioException catch (retryError) {
            return handler.next(retryError);
          }
        },
      ),
    );

    // Add logging interceptor in debug mode
    dio.interceptors.add(
      LogInterceptor(
        request: true,
        // Never print bearer tokens or other request headers to logcat.
        requestHeader: false,
        // Authentication payloads, learner transcripts, and AI feedback can
        // contain sensitive data. Keep diagnostics at metadata level only.
        requestBody: false,
        responseBody: false,
        responseHeader: false,
      ),
    );
  }

  Future<bool> _refreshAccessToken() {
    if (_refreshInFlight != null) return _refreshInFlight!;

    _refreshInFlight = () async {
      final prefs = await SharedPreferences.getInstance();
      final refreshToken = prefs.getString('refresh_token');
      if (refreshToken == null || refreshToken.isEmpty) {
        return false;
      }

      try {
        final response = await dio.post<Map<String, dynamic>>(
          '/auth/refresh',
          data: {'refreshToken': refreshToken},
          options: Options(extra: {'skipAuth': true}),
        );
        final body = response.data;
        final data = body?['data'];
        final accessToken = data?['accessToken'];
        final rotatedRefreshToken = data?['refreshToken'];

        if (body?['success'] != true ||
            accessToken is! String ||
            accessToken.isEmpty ||
            rotatedRefreshToken is! String ||
            rotatedRefreshToken.isEmpty) {
          await _clearTokens(prefs);
          return false;
        }

        await prefs.setString('access_token', accessToken);
        await prefs.setString('refresh_token', rotatedRefreshToken);
        return true;
      } on DioException {
        await _clearTokens(prefs);
        return false;
      } finally {
        _refreshInFlight = null;
      }
    }();

    return _refreshInFlight!;
  }

  Future<void> _clearTokens(SharedPreferences prefs) async {
    await prefs.remove('access_token');
    await prefs.remove('refresh_token');
  }
}

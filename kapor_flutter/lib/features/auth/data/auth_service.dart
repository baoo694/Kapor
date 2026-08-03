import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class AuthService {
  final Dio _dio = ApiClient().dio;

  Future<Map<String, dynamic>> loginWithEmail(
    String email,
    String password,
  ) async {
    try {
      final response = await _dio.post(
        '/auth/login',
        data: {'email': email, 'password': password},
      );
      return response.data;
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi đăng nhập');
    }
  }

  Future<Map<String, dynamic>> loginWithGoogle(String idToken) async {
    try {
      final response = await _dio.post(
        '/auth/google',
        data: {'idToken': idToken},
      );
      return response.data;
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Không thể đăng nhập với Google');
    }
  }

  Future<Map<String, dynamic>> register(
    String name,
    String email,
    String password,
  ) async {
    try {
      final response = await _dio.post(
        '/auth/register',
        data: {'name': name, 'email': email, 'password': password},
      );
      return response.data;
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi đăng ký');
    }
  }

  Future<void> forgotPassword(String email) async {
    try {
      await _dio.post('/auth/forgot-password', data: {'email': email});
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi gửi yêu cầu');
    }
  }

  Future<void> resetPassword(
    String email,
    String otp,
    String newPassword,
  ) async {
    try {
      await _dio.post(
        '/auth/reset-password',
        data: {'email': email, 'otp': otp, 'newPassword': newPassword},
      );
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi đổi mật khẩu');
    }
  }

  Future<void> completeOnboarding(List<String> goals, int dailyGoal) async {
    try {
      await _dio.put(
        '/users/me/onboarding',
        data: {'learningGoals': goals, 'dailyGoalMinutes': dailyGoal},
      );
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi lưu thông tin');
    }
  }

  Future<Map<String, dynamic>> updateSettings(
    Map<String, dynamic> settings,
  ) async {
    try {
      final response = await _dio.put(
        '/users/me',
        data: {'settings': settings},
      );
      if (response.data is Map &&
          response.data['success'] == true &&
          response.data['data'] is Map) {
        return Map<String, dynamic>.from(response.data['data'] as Map);
      }
      throw response.data is Map
          ? response.data['message'] ?? 'Không thể lưu cài đặt'
          : 'Không thể lưu cài đặt';
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Không thể lưu cài đặt');
    }
  }

  Future<Map<String, dynamic>> getCurrentUser() async {
    try {
      final response = await _dio.get('/users/me');
      if (response.data is Map<String, dynamic> &&
          response.data['success'] == true) {
        return response.data['data'];
      }
      if (response.data is Map<String, dynamic>) {
        throw response.data['message'] ?? 'Lỗi tải thông tin người dùng';
      }
      throw 'Lỗi tải thông tin người dùng';
    } on DioException catch (e) {
      throw _extractErrorMessage(e, 'Lỗi tải thông tin người dùng');
    }
  }

  String _extractErrorMessage(DioException e, String defaultMessage) {
    if (e.response != null && e.response?.data != null) {
      final data = e.response!.data;
      if (data is Map<String, dynamic>) {
        return data['message']?.toString() ??
            data['error']?.toString() ??
            defaultMessage;
      } else if (data is String && data.isNotEmpty) {
        return data;
      }
    }
    return defaultMessage;
  }
}

import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class AuthService {
  final Dio _dio = ApiClient().dio;

  Future<Map<String, dynamic>> loginWithEmail(String email, String password) async {
    try {
      final response = await _dio.post('/auth/login', data: {
        'email': email,
        'password': password,
      });
      return response.data;
    } on DioException catch (e) {
      if (e.response != null && e.response?.data != null) {
        throw e.response!.data['message'] ?? e.response!.data['error'] ?? 'Lỗi đăng nhập';
      }
      throw 'Lỗi kết nối máy chủ';
    }
  }

  Future<Map<String, dynamic>> register(String name, String email, String password) async {
    try {
      final response = await _dio.post('/auth/register', data: {
        'name': name,
        'email': email,
        'password': password,
      });
      return response.data;
    } on DioException catch (e) {
      if (e.response != null && e.response?.data != null) {
        throw e.response!.data['message'] ?? e.response!.data['error'] ?? 'Lỗi đăng ký';
      }
      throw 'Lỗi kết nối máy chủ';
    }
  }

  Future<void> forgotPassword(String email) async {
    try {
      await _dio.post('/auth/forgot-password', data: {
        'email': email,
      });
    } on DioException catch (e) {
      if (e.response != null && e.response?.data != null) {
        throw e.response!.data['message'] ?? e.response!.data['error'] ?? 'Lỗi gửi yêu cầu';
      }
      throw 'Lỗi kết nối máy chủ';
    }
  }

  Future<void> resetPassword(String email, String otp, String newPassword) async {
    try {
      await _dio.post('/auth/reset-password', data: {
        'email': email,
        'otp': otp,
        'newPassword': newPassword,
      });
    } on DioException catch (e) {
      if (e.response != null && e.response?.data != null) {
        throw e.response!.data['message'] ?? e.response!.data['error'] ?? 'Lỗi đổi mật khẩu';
      }
      throw 'Lỗi kết nối máy chủ';
    }
  }

  Future<void> completeOnboarding(List<String> goals, String level, int dailyGoal) async {
    try {
      await _dio.put('/users/me/onboarding', data: {
        'learningGoals': goals,
        'koreanLevel': level,
        'dailyGoalMinutes': dailyGoal,
      });
    } on DioException catch (e) {
      if (e.response != null && e.response?.data != null) {
        throw e.response!.data['message'] ?? e.response!.data['error'] ?? 'Lỗi lưu thông tin';
      }
      throw 'Lỗi kết nối máy chủ';
    }
  }
}

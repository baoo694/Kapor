import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  
  bool _isLoading = false;
  bool get isLoading => _isLoading;
  
  bool _isAuthenticated = false;
  bool get isAuthenticated => _isAuthenticated;
  
  Map<String, dynamic>? _user;
  Map<String, dynamic>? get user => _user;

  AuthProvider() {
    _checkAuthStatus();
  }

  Future<void> _checkAuthStatus() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('access_token');
    if (token != null && token.isNotEmpty) {
      _isAuthenticated = true;
      // You can also load user data from preferences here if saved
      notifyListeners();
    }
  }

  Future<void> login(String email, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _authService.loginWithEmail(email, password);
      if (response['success'] == true) {
        final data = response['data'];
        final accessToken = data['accessToken'];
        final refreshToken = data['refreshToken'];
        
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('access_token', accessToken);
        if (refreshToken != null) {
          await prefs.setString('refresh_token', refreshToken);
        }
        
        _user = data['user'];
        _isAuthenticated = true;
      } else {
        throw response['message'] ?? 'Đăng nhập thất bại';
      }
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> register(String name, String email, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _authService.register(name, email, password);
      if (response['success'] != true) {
        throw response['message'] ?? 'Đăng ký thất bại';
      }
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> forgotPassword(String email) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.forgotPassword(email);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> resetPassword(String email, String otp, String newPassword) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.resetPassword(email, otp, newPassword);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> completeOnboarding(List<String> goals, String level, int dailyGoal) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.completeOnboarding(goals, level, dailyGoal);
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('has_completed_onboarding', true);
      
      if (_user != null) {
        _user!['hasCompletedOnboarding'] = true;
        _user!['learningGoals'] = goals;
        _user!['koreanLevel'] = level;
      }
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  bool get hasCompletedOnboarding {
    if (_user != null) {
      return _user!['hasCompletedOnboarding'] == true;
    }
    return false;
  }

  Future<void> fetchCurrentUser() async {
    _isLoading = true;
    notifyListeners();
    try {
      _user = await _authService.getCurrentUser();
    } catch (e) {
      debugPrint('Error fetching user profile: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('access_token');
    await prefs.remove('refresh_token');
    await prefs.remove('has_completed_onboarding');
    _isAuthenticated = false;
    _user = null;
    notifyListeners();
  }
}

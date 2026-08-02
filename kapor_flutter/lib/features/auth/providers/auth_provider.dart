import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/auth_service.dart';
import '../data/google_sign_in_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  final GoogleSignInService _googleSignInService = GoogleSignInService();

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
      notifyListeners();

      // The token only proves that a session may exist; load the profile from
      // the API so UI elements (for example, the dashboard greeting) always
      // use the current display name from the database.
      await fetchCurrentUser();
    }
  }

  Future<void> login(String email, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _authService.loginWithEmail(email, password);
      await _persistAuthenticatedSession(response, 'Đăng nhập thất bại');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Returns false when the user closes the Google account picker.
  Future<bool> loginWithGoogle() async {
    _isLoading = true;
    notifyListeners();

    try {
      final idToken = await _googleSignInService.authenticate();
      if (idToken == null) return false;

      final response = await _authService.loginWithGoogle(idToken);
      await _persistAuthenticatedSession(
        response,
        'Đăng nhập với Google thất bại',
      );
      return true;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> _persistAuthenticatedSession(
    Map<String, dynamic> response,
    String defaultError,
  ) async {
    if (response['success'] != true) {
      throw response['message'] ?? defaultError;
    }

    final data = response['data'];
    if (data is! Map) {
      throw defaultError;
    }

    final accessToken = data['accessToken'];
    final refreshToken = data['refreshToken'];
    final user = data['user'];
    if (accessToken is! String ||
        accessToken.isEmpty ||
        refreshToken is! String ||
        refreshToken.isEmpty ||
        user is! Map) {
      throw defaultError;
    }

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('access_token', accessToken);
    await prefs.setString('refresh_token', refreshToken);
    _user = Map<String, dynamic>.from(user);
    _isAuthenticated = true;
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

  Future<void> resetPassword(
    String email,
    String otp,
    String newPassword,
  ) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.resetPassword(email, otp, newPassword);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> completeOnboarding(List<String> goals, int dailyGoal) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.completeOnboarding(goals, dailyGoal);

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('has_completed_onboarding', true);

      if (_user != null) {
        _user!['hasCompletedOnboarding'] = true;
        _user!['learningGoals'] = goals;
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

  Future<void> fetchCurrentUser({bool showLoading = true}) async {
    if (showLoading) {
      _isLoading = true;
      notifyListeners();
    }
    try {
      _user = await _authService.getCurrentUser();
    } catch (e) {
      debugPrint('Error fetching user profile: $e');
    } finally {
      if (showLoading) _isLoading = false;
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
    try {
      await _googleSignInService.signOut();
    } catch (_) {
      // Local Kapor logout must still succeed if the Google SDK is unavailable.
    }
    notifyListeners();
  }
}

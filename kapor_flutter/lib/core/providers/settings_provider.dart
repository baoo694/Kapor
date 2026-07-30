import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsProvider extends ChangeNotifier {
  static const String _themeKey = 'theme_mode';
  static const String _localeKey = 'ui_locale';
  static const String _techTalkAutoTtsKey = 'techtalk_auto_tts';

  ThemeMode _themeMode =
      ThemeMode.dark; // Default to dark as per aesthetic stance
  String _localeCode = 'vi';
  bool _techTalkAutoTts = true;

  ThemeMode get themeMode => _themeMode;
  String get localeCode => _localeCode;
  bool get techTalkAutoTts => _techTalkAutoTts;

  SettingsProvider() {
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();

    // Load theme
    final themeString = prefs.getString(_themeKey);
    if (themeString == 'light') {
      _themeMode = ThemeMode.light;
    } else {
      _themeMode = ThemeMode.dark; // default or 'dark'
    }
    _localeCode = prefs.getString(_localeKey) == 'en' ? 'en' : 'vi';
    _techTalkAutoTts = prefs.getBool(_techTalkAutoTtsKey) ?? true;

    notifyListeners();
  }

  Future<void> toggleTheme() async {
    _themeMode = _themeMode == ThemeMode.dark
        ? ThemeMode.light
        : ThemeMode.dark;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_themeKey, _themeMode.name);
  }

  Future<void> setLocale(String value) async {
    _localeCode = value == 'en' ? 'en' : 'vi';
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_localeKey, _localeCode);
  }

  Future<void> setTechTalkAutoTts(bool value) async {
    _techTalkAutoTts = value;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_techTalkAutoTtsKey, value);
  }
}

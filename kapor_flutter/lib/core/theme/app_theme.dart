import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color background = Color(0xFF18181A);
  static const Color surface = Color(0xFF222225);
  static const Color primary = Color(0xFF00E5FF); // Teal/Cyan neon
  static const Color secondary = Color(0xFFFFB300); // Amber
  static const Color textPrimary = Color(0xFFF3F4F6); // Gray 100
  static const Color textSecondary = Color(0xFF9CA3AF); // Gray 400

  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: background,
      primaryColor: primary,
      colorScheme: const ColorScheme.dark(
        primary: primary,
        secondary: secondary,
        surface: surface,
        background: background,
        onBackground: textPrimary,
        onSurface: textPrimary,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        elevation: 0,
        centerTitle: true,
        iconTheme: const IconThemeData(color: textPrimary),
        titleTextStyle: GoogleFonts.outfit(
          color: textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.w600,
        ),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: surface,
        selectedItemColor: primary,
        unselectedItemColor: textSecondary,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.white.withOpacity(0.05)),
        ),
      ),
      textTheme: TextTheme(
        displayLarge: GoogleFonts.outfit(color: textPrimary, fontWeight: FontWeight.bold),
        displayMedium: GoogleFonts.outfit(color: textPrimary, fontWeight: FontWeight.bold),
        displaySmall: GoogleFonts.outfit(color: textPrimary, fontWeight: FontWeight.bold),
        headlineMedium: GoogleFonts.outfit(color: textPrimary, fontWeight: FontWeight.w600),
        titleLarge: GoogleFonts.outfit(color: textPrimary, fontWeight: FontWeight.w600),
        titleMedium: GoogleFonts.inter(color: textPrimary, fontWeight: FontWeight.w500),
        bodyLarge: GoogleFonts.inter(color: textPrimary),
        bodyMedium: GoogleFonts.inter(color: textPrimary),
        labelLarge: GoogleFonts.jetBrainsMono(color: textPrimary, fontWeight: FontWeight.w500),
      ),
    );
  }
  static ThemeData get lightTheme {
    return ThemeData(
      brightness: Brightness.light,
      scaffoldBackgroundColor: const Color(0xFFF9FAFB),
      primaryColor: primary,
      colorScheme: const ColorScheme.light(
        primary: primary,
        secondary: secondary,
        surface: Colors.white,
        background: Color(0xFFF9FAFB),
        onBackground: Color(0xFF111827),
        onSurface: Color(0xFF111827),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: const Color(0xFFF9FAFB),
        elevation: 0,
        centerTitle: true,
        iconTheme: const IconThemeData(color: Color(0xFF111827)),
        titleTextStyle: GoogleFonts.outfit(
          color: const Color(0xFF111827),
          fontSize: 20,
          fontWeight: FontWeight.w600,
        ),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: Colors.white,
        selectedItemColor: primary,
        unselectedItemColor: Color(0xFF6B7280),
        type: BottomNavigationBarType.fixed,
        elevation: 8,
      ),
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 2,
        shadowColor: Colors.black.withOpacity(0.05),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.black.withOpacity(0.05)),
        ),
      ),
      textTheme: TextTheme(
        displayLarge: GoogleFonts.outfit(color: const Color(0xFF111827), fontWeight: FontWeight.bold),
        displayMedium: GoogleFonts.outfit(color: const Color(0xFF111827), fontWeight: FontWeight.bold),
        displaySmall: GoogleFonts.outfit(color: const Color(0xFF111827), fontWeight: FontWeight.bold),
        headlineMedium: GoogleFonts.outfit(color: const Color(0xFF111827), fontWeight: FontWeight.w600),
        titleLarge: GoogleFonts.outfit(color: const Color(0xFF111827), fontWeight: FontWeight.w600),
        titleMedium: GoogleFonts.inter(color: const Color(0xFF111827), fontWeight: FontWeight.w500),
        bodyLarge: GoogleFonts.inter(color: const Color(0xFF111827)),
        bodyMedium: GoogleFonts.inter(color: const Color(0xFF111827)),
        labelLarge: GoogleFonts.jetBrainsMono(color: const Color(0xFF111827), fontWeight: FontWeight.w500),
      ),
    );
  }
}

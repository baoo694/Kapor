import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../core/theme/app_theme.dart';

class AuthInput extends StatelessWidget {
  final IconData icon;
  final String placeholder;
  final String value;
  final Function(String) onChanged;
  final bool obscureText;
  final Widget? suffixIcon;
  final TextInputType keyboardType;

  const AuthInput({
    super.key,
    required this.icon,
    required this.placeholder,
    required this.value,
    required this.onChanged,
    this.obscureText = false,
    this.suffixIcon,
    this.keyboardType = TextInputType.text,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1F1F24), // similar to oklch(0.14 0.025 250)
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFF33333A), // oklch(0.22 0.03 250)
        ),
      ),
      child: Row(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14.0, vertical: 14.0),
            child: Icon(
              icon,
              size: 20,
              color: const Color(0xFF8B8B9E), // oklch(0.55 0.03 250)
            ),
          ),
          Expanded(
            child: TextFormField(
              initialValue: value,
              onChanged: onChanged,
              obscureText: obscureText,
              keyboardType: keyboardType,
              style: GoogleFonts.inter(
                color: AppTheme.textPrimary,
                fontSize: 14,
              ),
              decoration: InputDecoration(
                hintText: placeholder,
                hintStyle: GoogleFonts.inter(
                  color: const Color(0xFF6B6B7A), // oklch(0.48 0.03 250)
                  fontSize: 14,
                ),
                border: InputBorder.none,
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(vertical: 14.0),
              ),
            ),
          ),
          if (suffixIcon != null)
            Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: suffixIcon!,
            ),
        ],
      ),
    );
  }
}

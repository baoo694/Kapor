import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AuthDivider extends StatelessWidget {
  const AuthDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24.0),
      child: Row(
        children: [
          Expanded(
            child: Container(
              height: 1,
              color: const Color(0xFF33333A), // oklch(0.22 0.03 250)
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Text(
              "HOẶC",
              style: GoogleFonts.jetBrainsMono(
                color: const Color(0xFF6B6B7A), // oklch(0.48 0.03 250)
                fontSize: 10,
                letterSpacing: 1.0,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Expanded(
            child: Container(
              height: 1,
              color: const Color(0xFF33333A),
            ),
          ),
        ],
      ),
    );
  }
}

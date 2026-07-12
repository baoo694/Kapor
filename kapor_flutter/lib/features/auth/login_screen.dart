import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_theme.dart';
import 'providers/auth_provider.dart';
import 'widgets/auth_button.dart';
import 'widgets/auth_divider.dart';
import 'widgets/auth_input.dart';
import 'widgets/google_auth_button.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _showPwd = false;
  bool _loading = false;
  String _err = '';

  Future<void> _handleLogin() async {
    setState(() => _err = '');
    if (_emailController.text.isEmpty || _passwordController.text.isEmpty) {
      setState(() => _err = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    setState(() => _loading = true);
    
    try {
      await context.read<AuthProvider>().login(
        _emailController.text,
        _passwordController.text,
      );
      if (mounted) {
        final authProvider = context.read<AuthProvider>();
        if (authProvider.hasCompletedOnboarding) {
          context.go('/dashboard');
        } else {
          context.go('/onboarding');
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _err = e.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            children: [
              // Header Gradient Area
              Container(
                width: double.infinity,
                padding: const EdgeInsets.only(top: 48, bottom: 28),
                decoration: BoxDecoration(
                  gradient: RadialGradient(
                    center: const Alignment(0, -1),
                    radius: 1.5,
                    colors: [
                      AppTheme.primary.withOpacity(0.18),
                      Colors.transparent,
                    ],
                    stops: const [0.0, 1.0],
                  ),
                ),
                child: Column(
                  children: [
                    Container(
                      width: 68,
                      height: 68,
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.18),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: AppTheme.primary.withOpacity(0.40),
                          width: 2,
                        ),
                      ),
                      alignment: Alignment.center,
                      margin: const EdgeInsets.only(bottom: 12),
                      child: const Text(
                        '🎓',
                        style: TextStyle(fontSize: 32),
                      ),
                    ),
                    Text(
                      'KAPOR',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w900,
                        fontSize: 28,
                        color: AppTheme.primary,
                        height: 1.0,
                        letterSpacing: -0.5,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'KOREAN IT · AI',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: const Color(0xFF55556B), // roughly oklch(0.40 0.03 250)
                        letterSpacing: 1.5,
                      ),
                    ),
                  ],
                ),
              ),

              // Form Area
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 28.0, vertical: 8.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Chào mừng trở lại 👋',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w700,
                        fontSize: 20,
                        color: AppTheme.textPrimary, // roughly oklch(0.92 0.01 250)
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Đăng nhập để tiếp tục học tập',
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: const Color(0xFF6B6B7A), // oklch(0.48 0.03 250)
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Inputs
                    AuthInput(
                      icon: LucideIcons.mail,
                      placeholder: 'Email của bạn',
                      value: _emailController.text,
                      keyboardType: TextInputType.emailAddress,
                      onChanged: (val) => _emailController.text = val,
                    ),
                    const SizedBox(height: 12),
                    AuthInput(
                      icon: LucideIcons.lock,
                      placeholder: 'Mật khẩu',
                      value: _passwordController.text,
                      obscureText: !_showPwd,
                      onChanged: (val) => _passwordController.text = val,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _showPwd ? LucideIcons.eyeOff : LucideIcons.eye,
                          color: const Color(0xFF636373), // oklch(0.42 0.03 250)
                          size: 16,
                        ),
                        onPressed: () => setState(() => _showPwd = !_showPwd),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        splashRadius: 20,
                      ),
                    ),

                    // Forgot Password
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: () => context.push('/forgot-password'),
                        style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 0),
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        ),
                        child: Text(
                          'Quên mật khẩu?',
                          style: GoogleFonts.inter(
                            fontSize: 12,
                            color: AppTheme.primary,
                          ),
                        ),
                      ),
                    ),

                    // Error message
                    if (_err.isNotEmpty)
                      Container(
                        width: double.infinity,
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF87171).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: const Color(0xFFF87171).withOpacity(0.25)),
                        ),
                        child: Text(
                          _err,
                          style: GoogleFonts.inter(
                            fontSize: 12,
                            color: const Color(0xFFF87171),
                          ),
                        ),
                      ),

                    // Login Button
                    AuthButton(
                      label: 'Đăng nhập',
                      onPressed: _handleLogin,
                      isLoading: _loading,
                      icon: LucideIcons.refreshCw,
                    ),

                    const AuthDivider(),

                    GoogleAuthButton(
                      label: 'Đăng nhập với Google',
                      onPressed: () {
                        context.go('/dashboard');
                      },
                    ),

                    const SizedBox(height: 24),

                    // Register Link
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Chưa có tài khoản? ',
                          style: GoogleFonts.inter(
                            fontSize: 13,
                            color: const Color(0xFF7B7B8C), // oklch(0.50 0.03 250)
                          ),
                        ),
                        GestureDetector(
                          onTap: () => context.push('/register'),
                          child: Text(
                            'Đăng ký ngay',
                            style: GoogleFonts.outfit(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppTheme.primary,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

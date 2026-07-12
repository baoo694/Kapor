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

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmController = TextEditingController();

  bool _showPwd = false;
  bool _showConfirm = false;
  bool _agreed = false;
  bool _loading = false;
  String _err = '';

  int get _strength {
    final pwd = _passwordController.text;
    if (pwd.isEmpty) return 0;
    if (pwd.length < 6) return 1;
    if (pwd.length < 8) return 2;
    if (RegExp(r'[A-Z]').hasMatch(pwd) && RegExp(r'[0-9!@#\$%\^&\*]').hasMatch(pwd)) return 4;
    return 3;
  }

  final List<Map<String, dynamic>> _strengthMeta = [
    {'label': '', 'color': const Color(0xFF33333A)},
    {'label': 'Yếu', 'color': const Color(0xFFF87171)},
    {'label': 'Trung bình', 'color': AppTheme.secondary},
    {'label': 'Khá mạnh', 'color': AppTheme.primary},
    {'label': 'Mạnh', 'color': const Color(0xFF34D399)},
  ];

  Future<void> _handleRegister() async {
    setState(() => _err = '');
    if (_nameController.text.isEmpty || _emailController.text.isEmpty || _passwordController.text.isEmpty) {
      setState(() => _err = 'Vui lòng điền đầy đủ thông tin.');
      return;
    }
    if (_passwordController.text != _confirmController.text) {
      setState(() => _err = 'Mật khẩu xác nhận không khớp.');
      return;
    }
    if (!_agreed) {
      setState(() => _err = 'Vui lòng đồng ý điều khoản sử dụng.');
      return;
    }
    setState(() => _loading = true);
    
    try {
      await context.read<AuthProvider>().register(
        _nameController.text,
        _emailController.text,
        _passwordController.text,
      );
      if (mounted) {
        // Automatically route to login after successful registration
        context.push('/login');
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
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                child: Row(
                  children: [
                    GestureDetector(
                      onTap: () => context.pop(),
                      child: Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: const Color(0xFF1F1F24),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: const Color(0xFF33333A)),
                        ),
                        child: const Icon(LucideIcons.chevronLeft, size: 18, color: Color(0xFFB0B0C0)),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'Tạo tài khoản',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w700,
                        fontSize: 17,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ],
                ),
              ),

              Padding(
                padding: const EdgeInsets.fromLTRB(28, 20, 28, 32),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Bắt đầu hành trình 🚀',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w700,
                        fontSize: 18,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Học IT tiếng Hàn cùng AI — miễn phí',
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: const Color(0xFF6B6B7A),
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Inputs
                    AuthInput(
                      icon: LucideIcons.user,
                      placeholder: 'Họ và tên',
                      value: _nameController.text,
                      onChanged: (val) => _nameController.text = val,
                    ),
                    const SizedBox(height: 12),
                    AuthInput(
                      icon: LucideIcons.mail,
                      placeholder: 'Email',
                      value: _emailController.text,
                      keyboardType: TextInputType.emailAddress,
                      onChanged: (val) => _emailController.text = val,
                    ),
                    const SizedBox(height: 12),

                    AuthInput(
                      icon: LucideIcons.lock,
                      placeholder: 'Mật khẩu (tối thiểu 8 ký tự)',
                      value: _passwordController.text,
                      obscureText: !_showPwd,
                      onChanged: (val) {
                        _passwordController.text = val;
                        setState(() {});
                      },
                      suffixIcon: IconButton(
                        icon: Icon(
                          _showPwd ? LucideIcons.eyeOff : LucideIcons.eye,
                          color: const Color(0xFF636373),
                          size: 16,
                        ),
                        onPressed: () => setState(() => _showPwd = !_showPwd),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        splashRadius: 20,
                      ),
                    ),
                    if (_passwordController.text.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Row(
                        children: List.generate(4, (index) {
                          int i = index + 1;
                          return Expanded(
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 300),
                              height: 3,
                              margin: EdgeInsets.only(right: index < 3 ? 4 : 0),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(2),
                                color: i <= _strength
                                    ? _strengthMeta[_strength]['color'] as Color
                                    : const Color(0xFF33333A),
                              ),
                            ),
                          );
                        }),
                      ),
                      const SizedBox(height: 4),
                      Align(
                        alignment: Alignment.centerRight,
                        child: Text(
                          _strengthMeta[_strength]['label'] as String,
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 11,
                            color: _strengthMeta[_strength]['color'] as Color,
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 12),

                    AuthInput(
                      icon: LucideIcons.lock,
                      placeholder: 'Xác nhận mật khẩu',
                      value: _confirmController.text,
                      obscureText: !_showConfirm,
                      onChanged: (val) {
                        _confirmController.text = val;
                        setState(() {});
                      },
                      suffixIcon: IconButton(
                        icon: Icon(
                          _showConfirm ? LucideIcons.eyeOff : LucideIcons.eye,
                          color: const Color(0xFF636373),
                          size: 16,
                        ),
                        onPressed: () => setState(() => _showConfirm = !_showConfirm),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        splashRadius: 20,
                      ),
                    ),
                    if (_confirmController.text.isNotEmpty && _passwordController.text != _confirmController.text)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(
                          'Mật khẩu không khớp',
                          style: GoogleFonts.inter(
                            fontSize: 11,
                            color: const Color(0xFFF87171),
                          ),
                        ),
                      ),

                    const SizedBox(height: 16),

                    // Agreement Checkbox
                    GestureDetector(
                      onTap: () => setState(() => _agreed = !_agreed),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 18,
                            height: 18,
                            margin: const EdgeInsets.only(top: 1, right: 10),
                            decoration: BoxDecoration(
                              color: _agreed ? AppTheme.primary.withOpacity(0.2) : Colors.transparent,
                              borderRadius: BorderRadius.circular(5),
                              border: Border.all(
                                color: _agreed ? AppTheme.primary : const Color(0xFF4B4B5A),
                                width: 2,
                              ),
                            ),
                            alignment: Alignment.center,
                            child: _agreed
                                ? const Icon(LucideIcons.check, size: 11, color: AppTheme.primary)
                                : null,
                          ),
                          Expanded(
                            child: RichText(
                              text: TextSpan(
                                style: GoogleFonts.inter(
                                  fontSize: 12,
                                  color: const Color(0xFF8B8B9E),
                                  height: 1.5,
                                ),
                                children: const [
                                  TextSpan(text: 'Tôi đồng ý với '),
                                  TextSpan(text: 'Điều khoản sử dụng', style: TextStyle(color: AppTheme.primary)),
                                  TextSpan(text: ' và '),
                                  TextSpan(text: 'Chính sách bảo mật', style: TextStyle(color: AppTheme.primary)),
                                  TextSpan(text: ' của Kapor'),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 16),

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

                    // Register Button
                    AuthButton(
                      label: 'Tạo tài khoản',
                      onPressed: _handleRegister,
                      isLoading: _loading,
                      icon: LucideIcons.refreshCw,
                    ),

                    const AuthDivider(),

                    GoogleAuthButton(
                      label: 'Đăng ký với Google',
                      onPressed: () {
                        context.go('/onboarding');
                      },
                    ),

                    const SizedBox(height: 24),

                    // Login Link
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Đã có tài khoản? ',
                          style: GoogleFonts.inter(
                            fontSize: 13,
                            color: const Color(0xFF7B7B8C),
                          ),
                        ),
                        GestureDetector(
                          onTap: () => context.go('/login'),
                          child: Text(
                            'Đăng nhập',
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

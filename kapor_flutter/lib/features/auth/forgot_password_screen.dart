import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_theme.dart';
import 'providers/auth_provider.dart';
import 'widgets/auth_button.dart';
import 'widgets/auth_input.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  int _step = 0;
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _otpController = TextEditingController();
  final TextEditingController _newPwdController = TextEditingController();
  final TextEditingController _confirmPwdController = TextEditingController();

  bool _showPwd = false;
  bool _loading = false;
  int _countdown = 0;
  Timer? _timer;

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    _newPwdController.dispose();
    _confirmPwdController.dispose();
    _timer?.cancel();
    super.dispose();
  }

  void _startCountdown() {
    setState(() => _countdown = 60);
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_countdown <= 1) {
        timer.cancel();
        setState(() => _countdown = 0);
      } else {
        setState(() => _countdown--);
      }
    });
  }

  Future<void> _handleSendOTP() async {
    setState(() => _loading = true);
    try {
      await context.read<AuthProvider>().forgotPassword(_emailController.text);
      if (mounted) {
        setState(() {
          _step = 1;
        });
        _startCountdown();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  void _handleVerify() {
    // Simply move to the next step (no backend verify API needed)
    setState(() => _loading = true);
    Future.delayed(const Duration(milliseconds: 400), () {
      if (mounted) {
        setState(() {
          _loading = false;
          _step = 2;
        });
      }
    });
  }

  Future<void> _handleReset() async {
    setState(() => _loading = true);
    try {
      await context.read<AuthProvider>().resetPassword(
        _emailController.text,
        _otpController.text,
        _newPwdController.text,
      );
      if (mounted) {
        context.go('/login');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  final List<Map<String, dynamic>> _stepMeta = [
    {'label': 'Xác nhận email', 'icon': LucideIcons.mail},
    {'label': 'Nhập mã OTP', 'icon': LucideIcons.shieldCheck},
    {'label': 'Mật khẩu mới', 'icon': LucideIcons.lock},
  ];

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
                      onTap: () {
                        if (_step > 0) {
                          setState(() => _step--);
                        } else {
                          context.pop();
                        }
                      },
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
                      'Quên mật khẩu',
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w700,
                        fontSize: 17,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ],
                ),
              ),

              // Step indicator
              Padding(
                padding: const EdgeInsets.fromLTRB(28, 24, 28, 0),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: List.generate(5, (index) {
                    if (index % 2 != 0) {
                      // Line
                      int stepIndex = index ~/ 2;
                      return Expanded(
                        child: Container(
                          height: 2,
                          margin: const EdgeInsets.only(top: 15, left: 4, right: 4),
                          color: stepIndex < _step ? AppTheme.primary : const Color(0xFF33333A),
                        ),
                      );
                    }
                    // Circle
                    int stepIndex = index ~/ 2;
                    bool isCompleted = stepIndex < _step;
                    bool isCurrent = stepIndex == _step;
                    return Column(
                      children: [
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 300),
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: isCompleted
                                ? AppTheme.primary.withOpacity(0.2)
                                : (isCurrent ? AppTheme.primary.withOpacity(0.3) : const Color(0xFF1F1F24)),
                            border: Border.all(
                              color: isCompleted || isCurrent ? AppTheme.primary : const Color(0xFF33333A),
                              width: 2,
                            ),
                          ),
                          alignment: Alignment.center,
                          child: isCompleted
                              ? const Icon(LucideIcons.check, size: 14, color: AppTheme.primary)
                              : Icon(
                                  _stepMeta[stepIndex]['icon'],
                                  size: 13,
                                  color: isCurrent ? AppTheme.primary : const Color(0xFF6B6B7A),
                                ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          (_stepMeta[stepIndex]['label'] as String).toUpperCase(),
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 9,
                            letterSpacing: 0.3,
                            color: isCurrent ? AppTheme.primary : const Color(0xFF55556B),
                          ),
                        ),
                      ],
                    );
                  }),
                ),
              ),

              Padding(
                padding: const EdgeInsets.fromLTRB(28, 32, 28, 32),
                child: Column(
                  children: [
                    if (_step == 0) ...[
                      // Step 0: Email
                      Container(
                        width: 60,
                        height: 60,
                        decoration: BoxDecoration(
                          color: AppTheme.primary.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: AppTheme.primary.withOpacity(0.30)),
                        ),
                        alignment: Alignment.center,
                        margin: const EdgeInsets.only(bottom: 14),
                        child: const Icon(LucideIcons.mail, size: 26, color: AppTheme.primary),
                      ),
                      Text(
                        'Xác nhận địa chỉ email',
                        style: GoogleFonts.outfit(
                          fontWeight: FontWeight.w700,
                          fontSize: 17,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Chúng tôi sẽ gửi mã xác nhận\nđến email của bạn',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          color: const Color(0xFF7B7B8C),
                          height: 1.6,
                        ),
                      ),
                      const SizedBox(height: 24),
                      AuthInput(
                        icon: LucideIcons.mail,
                        placeholder: 'Nhập email đã đăng ký',
                        value: _emailController.text,
                        keyboardType: TextInputType.emailAddress,
                        onChanged: (val) {
                          _emailController.text = val;
                          setState(() {});
                        },
                      ),
                      const SizedBox(height: 16),
                      AuthButton(
                        label: 'Gửi mã OTP →',
                        onPressed: _handleSendOTP,
                        isDisabled: _emailController.text.isEmpty,
                        isLoading: _loading,
                        icon: LucideIcons.refreshCw,
                      ),
                    ] else if (_step == 1) ...[
                      // Step 1: OTP
                      Container(
                        width: 60,
                        height: 60,
                        decoration: BoxDecoration(
                          color: AppTheme.primary.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: AppTheme.primary.withOpacity(0.30)),
                        ),
                        alignment: Alignment.center,
                        margin: const EdgeInsets.only(bottom: 14),
                        child: const Icon(LucideIcons.shieldCheck, size: 26, color: AppTheme.primary),
                      ),
                      Text(
                        'Nhập mã xác nhận',
                        style: GoogleFonts.outfit(
                          fontWeight: FontWeight.w700,
                          fontSize: 17,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 6),
                      RichText(
                        textAlign: TextAlign.center,
                        text: TextSpan(
                          style: GoogleFonts.inter(
                            fontSize: 12,
                            color: const Color(0xFF7B7B8C),
                            height: 1.6,
                          ),
                          children: [
                            const TextSpan(text: 'Mã 6 chữ số đã gửi đến\n'),
                            TextSpan(
                              text: _emailController.text,
                              style: GoogleFonts.jetBrainsMono(color: AppTheme.primary),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),

                      // OTP Boxes
                      SizedBox(
                        height: 52,
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: List.generate(6, (i) {
                                String char = _otpController.text.length > i ? _otpController.text[i] : '';
                                bool isFocused = _otpController.text.length == i;
                                return Container(
                                  width: 44,
                                  height: 52,
                                  margin: const EdgeInsets.symmetric(horizontal: 4),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFF1F1F24),
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(
                                      color: char.isNotEmpty ? AppTheme.primary : const Color(0xFF33333A),
                                      width: 2,
                                    ),
                                  ),
                                  alignment: Alignment.center,
                                  child: isFocused && char.isEmpty
                                      ? Container(
                                          width: 2,
                                          height: 22,
                                          color: AppTheme.primary,
                                        )
                                      : Text(
                                          char,
                                          style: GoogleFonts.outfit(
                                            fontWeight: FontWeight.w800,
                                            fontSize: 22,
                                            color: AppTheme.primary,
                                          ),
                                        ),
                                );
                              }),
                            ),
                            Positioned.fill(
                              child: TextField(
                                controller: _otpController,
                                keyboardType: TextInputType.number,
                                maxLength: 6,
                                showCursor: false,
                                cursorColor: Colors.transparent,
                                style: const TextStyle(color: Colors.transparent, fontSize: 24),
                                decoration: const InputDecoration(
                                  border: InputBorder.none,
                                  counterText: '',
                                  contentPadding: EdgeInsets.zero,
                                ),
                                onChanged: (val) => setState(() {}),
                              ),
                            ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 24),
                      AuthButton(
                        label: 'Xác nhận',
                        onPressed: _handleVerify,
                        isDisabled: _otpController.text.length < 6,
                        isLoading: _loading,
                        icon: LucideIcons.refreshCw,
                      ),
                      const SizedBox(height: 16),
                      if (_countdown > 0)
                        RichText(
                          text: TextSpan(
                            style: GoogleFonts.inter(
                              fontSize: 12,
                              color: const Color(0xFF6B6B7A),
                            ),
                            children: [
                              const TextSpan(text: 'Gửi lại sau '),
                              TextSpan(
                                text: '${_countdown}s',
                                style: GoogleFonts.jetBrainsMono(color: AppTheme.primary),
                              ),
                            ],
                          ),
                        )
                      else
                        GestureDetector(
                          onTap: () {
                            _otpController.clear();
                            _handleSendOTP();
                          },
                          child: Text(
                            'Gửi lại mã OTP',
                            style: GoogleFonts.outfit(
                              fontWeight: FontWeight.w700,
                              fontSize: 12,
                              color: AppTheme.primary,
                            ),
                          ),
                        ),
                    ] else ...[
                      // Step 2: New Password
                      Container(
                        width: 60,
                        height: 60,
                        decoration: BoxDecoration(
                          color: AppTheme.primary.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: AppTheme.primary.withOpacity(0.30)),
                        ),
                        alignment: Alignment.center,
                        margin: const EdgeInsets.only(bottom: 14),
                        child: const Icon(LucideIcons.lock, size: 26, color: AppTheme.primary),
                      ),
                      Text(
                        'Đặt mật khẩu mới',
                        style: GoogleFonts.outfit(
                          fontWeight: FontWeight.w700,
                          fontSize: 17,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Chọn mật khẩu mạnh để bảo vệ tài khoản',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          color: const Color(0xFF7B7B8C),
                        ),
                      ),
                      const SizedBox(height: 24),

                      AuthInput(
                        icon: LucideIcons.lock,
                        placeholder: 'Mật khẩu mới',
                        value: _newPwdController.text,
                        obscureText: !_showPwd,
                        onChanged: (val) {
                          _newPwdController.text = val;
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
                      const SizedBox(height: 12),
                      AuthInput(
                        icon: LucideIcons.lock,
                        placeholder: 'Xác nhận mật khẩu mới',
                        value: _confirmPwdController.text,
                        obscureText: !_showPwd,
                        onChanged: (val) {
                          _confirmPwdController.text = val;
                          setState(() {});
                        },
                      ),
                      const SizedBox(height: 24),

                      AuthButton(
                        label: 'Lưu thay đổi',
                        onPressed: _handleReset,
                        isDisabled: _newPwdController.text.isEmpty || _confirmPwdController.text.isEmpty || _newPwdController.text != _confirmPwdController.text,
                        isLoading: _loading,
                        icon: LucideIcons.check,
                      ),
                    ],
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

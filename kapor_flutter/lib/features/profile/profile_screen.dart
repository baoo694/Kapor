import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_theme.dart';
import '../auth/providers/auth_provider.dart';
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool _notif = true;
  String _ttsSpeed = "1.0×";
  String _lang = "vi";
  bool _showLogoutConfirm = false;

  final Color _teal = const Color(0xFF2DD4BF);
  final Color _purple = const Color(0xFFA78BFA);
  final Color _green = const Color(0xFF34D399);
  final Color _orange = const Color(0xFFFB923C);
  final Color _red = const Color(0xFFF87171);

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Scaffold(
          backgroundColor: AppTheme.background,
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.only(bottom: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _buildHeader(),
                  _buildUserInfoCard(),
                  const SizedBox(height: 12),
                  _buildStatsGrid(),
                  const SizedBox(height: 12),
                  _buildSettingsSection(),
                ],
              ),
            ),
          ),
        ),
        if (_showLogoutConfirm) _buildLogoutConfirmModal(),
      ],
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
      child: Text(
        _lang == 'vi' ? 'Hồ sơ' : 'Profile',
        style: GoogleFonts.outfit(
          fontWeight: FontWeight.w800,
          fontSize: 20,
          color: const Color(0xFFF3F4F6), // AppTheme.textPrimary
        ),
      ),
    );
  }

  Widget _buildUserInfoCard() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: _KCard(
        child: Row(
          children: [
            Container(
              width: 54,
              height: 54,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _teal.withOpacity(0.18),
                border: Border.all(color: _teal.withOpacity(0.40), width: 2),
              ),
              alignment: Alignment.center,
              child: const Text('👨‍💻', style: TextStyle(fontSize: 24)),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Nguyễn Văn A',
                    style: GoogleFonts.outfit(
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    'nguyen.van.a@gmail.com',
                    style: GoogleFonts.jetBrainsMono(
                      fontSize: 10,
                      color: AppTheme.textPrimary.withOpacity(0.45),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      _Badge(text: 'Beginner KO', color: _teal),
                      const SizedBox(width: 6),
                      _Badge(text: '🇻🇳 Việt', color: _purple),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatsGrid() {
    final stats = [
      {'label': _lang == 'vi' ? 'Phút học' : 'Study mins', 'value': '1,250', 'icon': LucideIcons.clock, 'color': _teal},
      {'label': _lang == 'vi' ? 'Thẻ đã ôn' : 'Cards reviewed', 'value': '3,400', 'icon': LucideIcons.brain, 'color': _purple},
      {'label': 'Roleplay', 'value': '45', 'icon': LucideIcons.messageSquare, 'color': _green},
      {'label': 'Video', 'value': '23', 'icon': LucideIcons.play, 'color': _orange},
    ];

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: GridView.count(
        crossAxisCount: 2,
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        childAspectRatio: 2.3,
        children: stats.map((s) {
          final color = s['color'] as Color;
          return _KCard(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(10),
                    color: color.withOpacity(0.18),
                  ),
                  alignment: Alignment.center,
                  child: Icon(s['icon'] as IconData, size: 14, color: color),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        s['value'] as String,
                        style: GoogleFonts.outfit(
                          fontWeight: FontWeight.w700,
                          fontSize: 17,
                          color: color,
                        ),
                      ),
                      Text(
                        s['label'] as String,
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 9,
                          color: AppTheme.textPrimary.withOpacity(0.40),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildSettingsSection() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text(
              _lang == 'vi' ? 'CÀI ĐẶT' : 'SETTINGS',
              style: GoogleFonts.jetBrainsMono(
                fontSize: 9,
                letterSpacing: 1,
                color: AppTheme.textPrimary.withOpacity(0.40),
              ),
            ),
          ),
          _KCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            margin: const EdgeInsets.only(bottom: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(LucideIcons.bell, size: 15, color: AppTheme.textPrimary.withOpacity(0.5)),
                    const SizedBox(width: 10),
                    Text(
                      _lang == 'vi' ? 'Thông báo' : 'Notifications',
                      style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
                    ),
                  ],
                ),
                GestureDetector(
                  onTap: () => setState(() => _notif = !_notif),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    width: 42,
                    height: 24,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: _notif ? _teal : AppTheme.textPrimary.withOpacity(0.2),
                    ),
                    child: Stack(
                      children: [
                        AnimatedPositioned(
                          duration: const Duration(milliseconds: 200),
                          curve: Curves.easeInOut,
                          top: 3,
                          left: _notif ? 21 : 3,
                          child: Container(
                            width: 18,
                            height: 18,
                            decoration: const BoxDecoration(
                              shape: BoxShape.circle,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          _KCard(
            padding: const EdgeInsets.all(12),
            margin: const EdgeInsets.only(bottom: 8),
            child: Column(
              children: [
                Row(
                  children: [
                    Icon(LucideIcons.volume2, size: 15, color: AppTheme.textPrimary.withOpacity(0.5)),
                    const SizedBox(width: 10),
                    Text(
                      _lang == 'vi' ? 'Tốc độ TTS' : 'TTS Speed',
                      style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: ["0.75×", "1.0×", "1.25×", "1.5×"].map((s) {
                    final isSelected = _ttsSpeed == s;
                    return Expanded(
                      child: GestureDetector(
                        onTap: () => setState(() => _ttsSpeed = s),
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 3),
                          padding: const EdgeInsets.symmetric(vertical: 5),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            color: isSelected ? _teal : AppTheme.textPrimary.withOpacity(0.1),
                          ),
                          alignment: Alignment.center,
                          child: Text(
                            s,
                            style: GoogleFonts.jetBrainsMono(
                              fontSize: 10,
                              color: isSelected ? Colors.black : AppTheme.textPrimary.withOpacity(0.5),
                            ),
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          _KCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            margin: const EdgeInsets.only(bottom: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(LucideIcons.globe, size: 15, color: AppTheme.textPrimary.withOpacity(0.5)),
                    const SizedBox(width: 10),
                    Text(
                      _lang == 'vi' ? 'Ngôn ngữ UI' : 'UI Language',
                      style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
                    ),
                  ],
                ),
                GestureDetector(
                  onTap: () => setState(() => _lang = _lang == 'vi' ? 'en' : 'vi'),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: _teal.withOpacity(0.4)),
                      color: _teal.withOpacity(0.12),
                    ),
                    child: Text(
                      _lang == 'vi' ? 'Tiếng Việt' : 'English',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 12,
                        color: _teal,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          _KCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(LucideIcons.moon, size: 15, color: AppTheme.textPrimary.withOpacity(0.5)),
                    const SizedBox(width: 10),
                    Text(
                      _lang == 'vi' ? 'Giao diện tối' : 'Dark Mode',
                      style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
                    ),
                  ],
                ),
                Container(
                  width: 42,
                  height: 24,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: _teal,
                  ),
                  alignment: Alignment.centerRight,
                  padding: const EdgeInsets.only(right: 3),
                  child: Container(
                    width: 18,
                    height: 18,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.black,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          GestureDetector(
            onTap: () => setState(() => _showLogoutConfirm = true),
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 13),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: _red.withOpacity(0.4)),
                color: _red.withOpacity(0.1),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(LucideIcons.logOut, size: 15, color: _red),
                  const SizedBox(width: 8),
                  Text(
                    _lang == 'vi' ? 'Đăng xuất' : 'Log out',
                    style: GoogleFonts.outfit(
                      fontWeight: FontWeight.w700,
                      fontSize: 14,
                      color: _red,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLogoutConfirmModal() {
    return Container(
      color: Colors.black.withOpacity(0.65),
      alignment: Alignment.bottomCenter,
      child: Material(
        color: Colors.transparent,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(20, 24, 20, 32),
          decoration: BoxDecoration(
            color: const Color(0xFF1E1E24), // Approximate oklch(0.13 0.025 250)
            borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
            border: Border(top: BorderSide(color: Colors.white.withOpacity(0.1))),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 20),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(2),
                  color: AppTheme.textPrimary.withOpacity(0.2),
                ),
              ),
              Container(
                width: 52,
                height: 52,
                margin: const EdgeInsets.only(bottom: 12),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  color: _red.withOpacity(0.18),
                  border: Border.all(color: _red.withOpacity(0.4)),
                ),
                alignment: Alignment.center,
                child: Icon(LucideIcons.logOut, size: 22, color: _red),
              ),
              Text(
                _lang == 'vi' ? 'Xác nhận đăng xuất' : 'Confirm logout',
                style: GoogleFonts.outfit(
                  fontWeight: FontWeight.w700,
                  fontSize: 17,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                _lang == 'vi'
                    ? 'Bạn sẽ cần đăng nhập lại để tiếp tục học.'
                    : "You'll need to sign in again to continue.",
                textAlign: TextAlign.center,
                style: GoogleFonts.inter(
                  fontSize: 13,
                  color: AppTheme.textPrimary.withOpacity(0.5),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Expanded(
                    child: GestureDetector(
                      onTap: () => setState(() => _showLogoutConfirm = false),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 13),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(12),
                          color: AppTheme.textPrimary.withOpacity(0.1),
                          border: Border.all(color: AppTheme.textPrimary.withOpacity(0.15)),
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          _lang == 'vi' ? 'Huỷ' : 'Cancel',
                          style: GoogleFonts.outfit(
                            fontWeight: FontWeight.w600,
                            fontSize: 14,
                            color: AppTheme.textPrimary.withOpacity(0.75),
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: GestureDetector(
                      onTap: () async {
                        setState(() => _showLogoutConfirm = false);
                        await context.read<AuthProvider>().logout();
                        if (context.mounted) {
                          context.go('/login');
                        }
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 13),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(12),
                          color: _red,
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          _lang == 'vi' ? 'Đăng xuất' : 'Log out',
                          style: GoogleFonts.outfit(
                            fontWeight: FontWeight.w700,
                            fontSize: 14,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _KCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;

  const _KCard({
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: child,
    );
  }
}

class _Badge extends StatelessWidget {
  final String text;
  final Color color;

  const _Badge({required this.text, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Text(
        text,
        style: GoogleFonts.inter(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

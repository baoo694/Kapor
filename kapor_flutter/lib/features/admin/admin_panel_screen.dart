import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';

class AdminPanelScreen extends StatefulWidget {
  const AdminPanelScreen({super.key});

  @override
  State<AdminPanelScreen> createState() => _AdminPanelScreenState();
}

class _AdminPanelScreenState extends State<AdminPanelScreen> {
  String _selectedSection = 'dashboard';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: Row(
        children: [
          // Sidebar
          Container(
            width: 228,
            color: AppTheme.surface.withOpacity(0.5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 40, 20, 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'KAPOR',
                        style: GoogleFonts.outfit(
                          fontWeight: FontWeight.w800,
                          fontSize: 20,
                          color: AppTheme.primary,
                        ),
                      ),
                      Text(
                        'ADMIN PANEL',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                          letterSpacing: 1,
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    children: [
                      _buildSidebarItem('dashboard', Icons.bar_chart, 'Dashboard'),
                      _buildSidebarItem('users', Icons.people, 'Users'),
                      const Padding(
                        padding: EdgeInsets.fromLTRB(20, 16, 20, 8),
                        child: Text(
                          'CONTENT',
                          style: TextStyle(
                            fontSize: 10,
                            color: Colors.grey,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                      ),
                      _buildSidebarItem('content-topics', Icons.topic, 'Topics'),
                      _buildSidebarItem('content-lessons', Icons.book, 'Lessons'),
                      _buildSidebarItem('content-videos', Icons.video_library, 'Videos'),
                      _buildSidebarItem('content-scenarios', Icons.chat, 'Scenarios'),
                      const Padding(
                        padding: EdgeInsets.fromLTRB(20, 16, 20, 8),
                        child: Text(
                          'SETTINGS',
                          style: TextStyle(
                            fontSize: 10,
                            color: Colors.grey,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                      ),
                      _buildSidebarItem('settings-prompts', Icons.electric_bolt, 'AI Prompts'),
                      _buildSidebarItem('settings-admins', Icons.admin_panel_settings, 'Admin Users'),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'admin@kapor.app',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 11,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      Text(
                        'Super Admin',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 9,
                          color: AppTheme.textSecondary.withOpacity(0.7),
                        ),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () => context.go('/dashboard'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.surface,
                          foregroundColor: AppTheme.primary,
                          elevation: 0,
                        ),
                        child: const Text('Thoát Admin'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          
          // Main Content
          Expanded(
            child: _selectedSection == 'dashboard' ? _buildDashboard() : _buildPlaceholder(),
          ),
        ],
      ),
    );
  }

  Widget _buildSidebarItem(String id, IconData icon, String label) {
    final isSelected = _selectedSection == id;
    return InkWell(
      onTap: () {
        setState(() {
          _selectedSection = id;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? AppTheme.primary.withOpacity(0.1) : Colors.transparent,
          border: Border(
            left: BorderSide(
              color: isSelected ? AppTheme.primary : Colors.transparent,
              width: 3,
            ),
          ),
        ),
        child: Row(
          children: [
            Icon(icon, size: 18, color: isSelected ? AppTheme.primary : AppTheme.textSecondary),
            const SizedBox(width: 12),
            Text(
              label,
              style: GoogleFonts.inter(
                fontSize: 13,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                color: isSelected ? AppTheme.primary : AppTheme.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboard() {
    return ListView(
      padding: const EdgeInsets.all(32),
      children: [
        Text(
          'Dashboard',
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.w800,
            fontSize: 24,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            _buildKpiCard('Total Users', '1,234', Icons.people, AppTheme.primary, '+12% this month'),
            const SizedBox(width: 16),
            _buildKpiCard('DAU', '156', Icons.trending_up, const Color(0xFF34D399), '+8% vs last week'),
            const SizedBox(width: 16),
            _buildKpiCard('MAU', '890', Icons.bar_chart, const Color(0xFFA78BFA), '72% retention'),
            const SizedBox(width: 16),
            _buildKpiCard('Content', '523', Icons.book, AppTheme.secondary, '48 added this week'),
          ],
        ),
      ],
    );
  }

  Widget _buildKpiCard(String label, String value, IconData icon, Color color, String sub) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppTheme.surface.withOpacity(0.8),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppTheme.textSecondary.withOpacity(0.2)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: color.withOpacity(0.18),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, size: 16, color: color),
            ),
            const SizedBox(height: 12),
            Text(
              value,
              style: GoogleFonts.outfit(
                fontWeight: FontWeight.w800,
                fontSize: 28,
                color: color,
              ),
            ),
            Text(
              label,
              style: GoogleFonts.inter(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              sub,
              style: GoogleFonts.jetBrainsMono(
                fontSize: 10,
                color: AppTheme.textSecondary.withOpacity(0.7),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return Center(
      child: Text(
        '$_selectedSection is not implemented yet',
        style: GoogleFonts.jetBrainsMono(color: AppTheme.textSecondary),
      ),
    );
  }
}

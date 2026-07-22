import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';

class TechTalkSelectScreen extends StatefulWidget {
  const TechTalkSelectScreen({super.key});

  @override
  State<TechTalkSelectScreen> createState() => _TechTalkSelectScreenState();
}

class _TechTalkSelectScreenState extends State<TechTalkSelectScreen> {
  final _service = TechTalkService();
  late Future<List<TechTalkScenario>> _future;

  @override
  void initState() {
    super.initState();
    _future = _service.scenarios();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: context.pop,
        ),
        title: const Text('Chọn tình huống'),
      ),
      body: FutureBuilder<List<TechTalkScenario>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: TextButton(
                onPressed: () => setState(() => _future = _service.scenarios()),
                child: Text(
                  snapshot.error.toString().replaceFirst('Exception: ', ''),
                ),
              ),
            );
          }
          final scenarios = snapshot.data ?? const <TechTalkScenario>[];
          if (scenarios.isEmpty) {
            return Center(
              child: Text(
                'Chưa có scenario. Hãy thêm nội dung từ Admin Panel.',
                style: GoogleFonts.inter(color: AppTheme.textSecondary),
              ),
            );
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'CHỌN TÌNH HUỐNG LUYỆN TẬP',
                style: GoogleFonts.jetBrainsMono(
                  fontSize: 10,
                  color: AppTheme.textSecondary,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 12),
              ...scenarios.map(_card),
            ],
          );
        },
      ),
    );
  }

  Widget _card(TechTalkScenario scenario) {
    return Card(
      child: InkWell(
        onTap: () => context.push('/techtalk-chat', extra: scenario),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    scenario.persona.avatar,
                    style: const TextStyle(fontSize: 26),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          scenario.title,
                          style: GoogleFonts.outfit(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          scenario.titleVi,
                          style: GoogleFonts.inter(
                            fontSize: 12,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  _badge(scenario.difficulty),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                '🎯 ${scenario.missionVi}',
                style: GoogleFonts.inter(
                  fontSize: 12,
                  color: AppTheme.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '${scenario.persona.name} · ${scenario.persona.role} @ ${scenario.persona.company}',
                style: GoogleFonts.jetBrainsMono(
                  fontSize: 10,
                  color: AppTheme.primary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _badge(String value) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
    decoration: BoxDecoration(
      color: AppTheme.secondary.withOpacity(.15),
      borderRadius: BorderRadius.circular(6),
    ),
    child: Text(
      value,
      style: GoogleFonts.jetBrainsMono(fontSize: 9, color: AppTheme.secondary),
    ),
  );
}

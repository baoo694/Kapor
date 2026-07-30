import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../core/providers/settings_provider.dart';
import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';
import 'techtalk_screen.dart';
import 'techtalk_strings.dart';

class TechTalkHistoryScreen extends StatefulWidget {
  const TechTalkHistoryScreen({super.key});

  @override
  State<TechTalkHistoryScreen> createState() => _TechTalkHistoryScreenState();
}

class _TechTalkHistoryScreenState extends State<TechTalkHistoryScreen> {
  final _service = TechTalkService();
  late Future<_HistoryData> _future;
  bool _loadingMore = false;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<_HistoryData> _load() async {
    final values = await Future.wait([
      _service.history(size: 20),
      _service.scenarios(),
    ]);
    return _HistoryData(
      history: values[0] as RoleplayHistoryPage,
      scenarios: values[1] as List<TechTalkScenario>,
    );
  }

  Future<_HistoryData> _loadMore(_HistoryData current) async {
    setState(() => _loadingMore = true);
    try {
      final next = await _service.history(
        page: current.history.page + 1,
        size: current.history.size,
      );
      return _HistoryData(
        history: RoleplayHistoryPage(
          content: [...current.history.content, ...next.content],
          page: next.page,
          size: next.size,
          hasMore: next.hasMore,
        ),
        scenarios: current.scenarios,
      );
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = TechTalkStrings(
      context.watch<SettingsProvider>().localeCode,
    );
    return Scaffold(
      appBar: AppBar(title: Text(strings.history)),
      body: FutureBuilder<_HistoryData>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done &&
              !snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: TextButton(
                onPressed: () => setState(() => _future = _load()),
                child: Text(
                  snapshot.error.toString().replaceFirst('Exception: ', ''),
                ),
              ),
            );
          }
          final data = snapshot.data!;
          if (data.history.content.isEmpty) {
            return Center(child: Text(strings.emptyHistory));
          }
          return RefreshIndicator(
            onRefresh: () async {
              final future = _load();
              setState(() => _future = future);
              await future;
            },
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount:
                  data.history.content.length + (data.history.hasMore ? 1 : 0),
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                if (index == data.history.content.length) {
                  return Center(
                    child: TextButton(
                      onPressed: _loadingMore
                          ? null
                          : () {
                              final future = _loadMore(data);
                              setState(() => _future = future);
                            },
                      child: _loadingMore
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(strings.loadMore),
                    ),
                  );
                }
                final session = data.history.content[index];
                final scenario = data.scenarios
                    .where((item) => item.id == session.scenarioId)
                    .firstOrNull;
                return Card(
                  child: ListTile(
                    leading: Icon(
                      session.status == 'completed'
                          ? Icons.check_circle
                          : session.status == 'active'
                          ? Icons.play_circle
                          : Icons.cancel_outlined,
                      color: session.status == 'completed'
                          ? AppTheme.primary
                          : session.status == 'active'
                          ? AppTheme.secondary
                          : AppTheme.textSecondary,
                    ),
                    title: Text(
                      scenario?.titleVi ??
                          scenario?.title ??
                          session.scenarioId,
                    ),
                    subtitle: Text(
                      '${_status(session.status, strings)} · '
                      '${session.messages.where((message) => message.role == 'user').length} turns'
                      '${session.finalEvaluation == null ? '' : ' · ${session.finalEvaluation!.overallScore}/100'}',
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    onTap:
                        session.status == 'active' && scenario == null ||
                            session.status != 'active' &&
                                session.finalEvaluation == null
                        ? null
                        : () {
                            if (session.status == 'active' &&
                                scenario != null) {
                              context.push(
                                '/techtalk-chat',
                                extra: TechTalkChatArgs(
                                  scenario: scenario,
                                  session: session,
                                ),
                              );
                            } else if (session.finalEvaluation != null) {
                              context.push('/techtalk-result', extra: session);
                            }
                          },
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  String _status(String value, TechTalkStrings strings) => switch (value) {
    'completed' => strings.completed,
    'active' => strings.active,
    _ => strings.abandoned,
  };
}

class _HistoryData {
  const _HistoryData({required this.history, required this.scenarios});
  final RoleplayHistoryPage history;
  final List<TechTalkScenario> scenarios;
}

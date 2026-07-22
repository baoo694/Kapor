import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';

class TechTalkScreen extends StatefulWidget {
  const TechTalkScreen({super.key, required this.scenario});
  final TechTalkScenario scenario;
  @override
  State<TechTalkScreen> createState() => _TechTalkScreenState();
}

class _TechTalkScreenState extends State<TechTalkScreen> {
  final _service = TechTalkService();
  final _input = TextEditingController();
  RoleplaySession? _session;
  RoleplayHint? _hint;
  String? _error;
  bool _loading = true;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _start();
  }

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    try {
      final session = await _service.start(widget.scenario.id);
      if (mounted)
        setState(() {
          _session = session;
          _loading = false;
        });
    } catch (error) {
      if (mounted)
        setState(() {
          _error = error.toString().replaceFirst('Exception: ', '');
          _loading = false;
        });
    }
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    final session = _session;
    if (text.isEmpty || session == null || _sending) return;
    setState(() => _sending = true);
    _input.clear();
    try {
      final updated = await _service.send(session.id, text);
      if (mounted) setState(() => _session = updated);
    } catch (error) {
      if (mounted) {
        _input.text = text;
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _hintRequest() async {
    final session = _session;
    if (session == null) return;
    try {
      final hint = await _service.hint(session.id);
      if (mounted) setState(() => _hint = hint);
    } catch (error) {
      if (mounted)
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
    }
  }

  Future<void> _end() async {
    final session = _session;
    if (session == null) return;
    try {
      final result = await _service.end(session.id);
      if (mounted) context.pushReplacement('/techtalk-result', extra: result);
    } catch (error) {
      if (mounted)
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading)
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    if (_session == null)
      return Scaffold(
        appBar: AppBar(),
        body: Center(
          child: Text(_error ?? 'Không thể bắt đầu phiên roleplay.'),
        ),
      );
    final scenario = widget.scenario;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('TechTalk AI'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: context.pop,
        ),
        actions: [TextButton(onPressed: _end, child: const Text('Kết thúc'))],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(14),
              color: AppTheme.surface,
              child: Row(
                children: [
                  Text(
                    scenario.persona.avatar,
                    style: const TextStyle(fontSize: 26),
                  ),
                  const SizedBox(width: 9),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          scenario.persona.name,
                          style: GoogleFonts.outfit(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          '${scenario.persona.role} · ${scenario.persona.company}',
                          style: GoogleFonts.inter(
                            fontSize: 11,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Chip(label: Text(scenario.difficulty)),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(14),
                children: [
                  Text(
                    '🎯 ${scenario.missionVi}',
                    style: GoogleFonts.inter(
                      fontSize: 12,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 12),
                  ..._session!.messages.map(_message),
                  if (_hint != null) _hintCard(_hint!),
                ],
              ),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: Text(
                  _error!,
                  style: const TextStyle(color: Colors.redAccent),
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 4, 12, 12),
              child: Row(
                children: [
                  IconButton(
                    onPressed: _hintRequest,
                    tooltip: 'Gợi ý',
                    icon: const Icon(
                      Icons.lightbulb_outline,
                      color: AppTheme.secondary,
                    ),
                  ),
                  Expanded(
                    child: TextField(
                      controller: _input,
                      onSubmitted: (_) => _send(),
                      decoration: InputDecoration(
                        hintText: 'Nhập câu tiếng Hàn...',
                        filled: true,
                        fillColor: AppTheme.surface,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide.none,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    onPressed: _sending ? null : _send,
                    icon: _sending
                        ? const CircularProgressIndicator(strokeWidth: 2)
                        : const Icon(Icons.send, color: AppTheme.primary),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _message(RoleplayMessage message) {
    final user = message.role == 'user';
    final evaluation = message.evaluation;
    return Align(
      alignment: user ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        constraints: const BoxConstraints(maxWidth: 320),
        child: Column(
          crossAxisAlignment: user
              ? CrossAxisAlignment.end
              : CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: user ? AppTheme.primary : AppTheme.surface,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text(
                message.content,
                style: GoogleFonts.inter(
                  color: user ? Colors.black : AppTheme.textPrimary,
                  height: 1.45,
                ),
              ),
            ),
            if (evaluation != null)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  'G ${evaluation.grammar} · V ${evaluation.vocabulary} · P ${evaluation.politeness}',
                  style: GoogleFonts.jetBrainsMono(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _hintCard(RoleplayHint hint) => Card(
    color: AppTheme.secondary.withOpacity(.1),
    child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'GỢI Ý',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 10,
              color: AppTheme.secondary,
            ),
          ),
          Text(hint.keywords.join(' · ')),
          const SizedBox(height: 4),
          Text(hint.sentenceStructure),
          const SizedBox(height: 4),
          Text(
            hint.politenessTip,
            style: GoogleFonts.inter(
              fontSize: 11,
              color: AppTheme.textSecondary,
            ),
          ),
        ],
      ),
    ),
  );
}

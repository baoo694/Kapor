import 'dart:async';
import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:record/record.dart';

import '../../core/audio/korean_tts_player.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/theme/app_theme.dart';
import 'data/techtalk_service.dart';
import 'techtalk_strings.dart';

class TechTalkChatArgs {
  const TechTalkChatArgs({required this.scenario, this.session});
  final TechTalkScenario scenario;
  final RoleplaySession? session;
}

class TechTalkScreen extends StatefulWidget {
  const TechTalkScreen({
    super.key,
    required this.scenario,
    this.initialSession,
  });
  final TechTalkScenario scenario;
  final RoleplaySession? initialSession;

  @override
  State<TechTalkScreen> createState() => _TechTalkScreenState();
}

class _TechTalkScreenState extends State<TechTalkScreen> {
  final _service = TechTalkService();
  final _input = TextEditingController();
  final _scrollController = ScrollController();
  final _recorder = AudioRecorder();
  final _audioBytes = BytesBuilder(copy: false);
  final _random = Random.secure();

  StreamSubscription<Uint8List>? _audioSubscription;
  Timer? _recordingTimer;
  RoleplaySession? _session;
  List<RoleplayMessage> _messages = const [];
  RoleplayHint? _hint;
  RoleplayTranscription? _pendingVoice;
  List<double> _waveform = const [];
  String? _error;
  String? _lastFailedText;
  String? _lastFailedClientTurnId;
  String? _lastFailedAudioId;
  String? _lastFailedTranscript;
  bool _loading = true;
  bool _sending = false;
  bool _ending = false;
  bool _hintLoading = false;
  bool _recording = false;
  bool _transcribing = false;
  bool _allowPop = false;

  @override
  void initState() {
    super.initState();
    if (widget.initialSession != null) {
      _session = widget.initialSession;
      _messages = List.of(widget.initialSession!.messages);
      _loading = false;
    } else {
      unawaited(_start());
    }
  }

  @override
  void dispose() {
    _audioSubscription?.cancel();
    _recordingTimer?.cancel();
    _recorder.dispose();
    _input.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    try {
      final session = await _service.start(widget.scenario.id);
      if (!mounted) return;
      setState(() {
        _session = session;
        _messages = List.of(session.messages);
        _loading = false;
      });
      _scrollToBottom();
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = _errorMessage(error);
        _loading = false;
      });
    }
  }

  Future<void> _send({String? retryClientTurnId}) async {
    final text = _input.text.trim();
    final session = _session;
    if (text.isEmpty || session == null || _sending || _recording) return;
    final clientTurnId = retryClientTurnId ?? _clientTurnId();
    final temporaryAiId = 'streaming-$clientTurnId';
    final source = _pendingVoice == null ? 'text' : 'voice';
    final transcript = _pendingVoice?.transcript;
    final audioId = _pendingVoice?.audioId;
    final optimisticUser = RoleplayMessage(
      id: 'local-$clientTurnId',
      clientTurnId: clientTurnId,
      role: 'user',
      content: text,
      source: source,
      transcript: transcript,
      audioId: audioId,
      generationStatus: 'completed',
      timestamp: DateTime.now(),
    );
    final streamingAi = RoleplayMessage(
      id: temporaryAiId,
      role: 'ai',
      content: '',
      source: 'ai',
      generationStatus: 'streaming',
      timestamp: DateTime.now(),
    );
    setState(() {
      _sending = true;
      _error = null;
      _lastFailedText = null;
      _lastFailedClientTurnId = null;
      _messages = [..._messages, optimisticUser, streamingAi];
      _input.clear();
      _pendingVoice = null;
    });
    _scrollToBottom();

    try {
      await for (final event in _service.streamTurn(
        sessionId: session.id,
        clientTurnId: clientTurnId,
        content: text,
        source: source,
        transcript: transcript,
        audioId: audioId,
      )) {
        if (!mounted) return;
        switch (event.type) {
          case 'turn.accepted':
            _replaceMessage(
              'local-$clientTurnId',
              optimisticUser.copyWith(id: event.userMessageId),
            );
            break;
          case 'token':
            final current = _findMessage(temporaryAiId);
            if (current != null) {
              _replaceMessage(
                temporaryAiId,
                current.copyWith(
                  content: current.content + (event.delta ?? ''),
                ),
              );
            }
            break;
          case 'evaluation':
            final user = _findMessage(event.messageId ?? '');
            if (user != null) {
              _replaceMessage(
                user.id,
                user.copyWith(evaluation: event.evaluation),
              );
            }
            break;
          case 'message.completed':
            if (event.message != null) {
              _replaceMessage(temporaryAiId, event.message!);
              final settings = context.read<SettingsProvider>();
              if (settings.techTalkAutoTts) {
                unawaited(_playTts(event.message!.content, silent: true));
              }
            }
            break;
          case 'error':
            final failed = _findMessage(temporaryAiId);
            if (failed != null) {
              _replaceMessage(
                temporaryAiId,
                failed.copyWith(generationStatus: 'failed'),
              );
            }
            setState(() {
              _error = event.messageText ?? 'TechTalk AI unavailable.';
              _lastFailedText = text;
              _lastFailedClientTurnId = clientTurnId;
              _lastFailedAudioId = audioId;
              _lastFailedTranscript = transcript;
            });
            break;
        }
        _scrollToBottom();
      }
      final synced = await _service.detail(session.id);
      if (mounted) {
        setState(() {
          _session = synced;
          _messages = List.of(synced.messages);
        });
      }
    } catch (error) {
      if (!mounted) return;
      final failed = _findMessage(temporaryAiId);
      if (failed != null) {
        _replaceMessage(
          temporaryAiId,
          failed.copyWith(generationStatus: 'failed'),
        );
      }
      setState(() {
        _error = _errorMessage(error);
        _lastFailedText = text;
        _lastFailedClientTurnId = clientTurnId;
        _lastFailedAudioId = audioId;
        _lastFailedTranscript = transcript;
      });
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  void _retryFailed() {
    final text = _lastFailedText;
    final clientTurnId = _lastFailedClientTurnId;
    if (text == null || text.isEmpty) return;
    setState(() {
      _messages = _messages
          .where(
            (message) =>
                message.generationStatus != 'failed' &&
                !(message.role == 'user' && message.content == text),
          )
          .toList();
      _input.text = text;
      if (_lastFailedAudioId != null) {
        _pendingVoice = RoleplayTranscription(
          audioId: _lastFailedAudioId!,
          transcript: _lastFailedTranscript ?? text,
          confidence: null,
          durationMs: 0,
        );
      }
      _error = null;
    });
    unawaited(_send(retryClientTurnId: clientTurnId));
  }

  Future<void> _hintRequest() async {
    final session = _session;
    if (session == null || _hintLoading) return;
    setState(() => _hintLoading = true);
    try {
      final hint = await _service.hint(session.id);
      if (mounted) setState(() => _hint = hint);
    } catch (error) {
      if (mounted) setState(() => _error = _errorMessage(error));
    } finally {
      if (mounted) setState(() => _hintLoading = false);
    }
  }

  Future<void> _end() async {
    final session = _session;
    if (session == null || _ending || _sending) return;
    setState(() => _ending = true);
    try {
      final result = await _service.end(session.id);
      if (mounted) context.pushReplacement('/techtalk-result', extra: result);
    } catch (error) {
      if (mounted) setState(() => _error = _errorMessage(error));
    } finally {
      if (mounted) setState(() => _ending = false);
    }
  }

  Future<void> _requestLeave() async {
    if (_allowPop) return;
    final strings = TechTalkStrings(
      context.read<SettingsProvider>().localeCode,
    );
    final action = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(strings.title),
        content: Text(strings.leaveQuestion),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, 'continue'),
            child: Text(strings.continueSession),
          ),
          TextButton(
            onPressed: _sending
                ? null
                : () => Navigator.pop(dialogContext, 'abandon'),
            child: Text(strings.abandon),
          ),
          if (!_sending)
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, 'end'),
              child: Text(strings.end),
            ),
        ],
      ),
    );
    if (!mounted || action == null || action == 'continue') return;
    if (action == 'end') {
      await _end();
      return;
    }
    final session = _session;
    if (session != null) {
      try {
        await _service.abandon(session.id);
      } catch (_) {
        // Leaving the local screen must remain possible if the network drops.
      }
    }
    if (!mounted) return;
    setState(() => _allowPop = true);
    context.pop();
  }

  Future<void> _toggleRecording() async {
    if (_recording) {
      await _stopAndTranscribe();
      return;
    }
    if (_sending || _transcribing) return;
    try {
      if (!await _recorder.hasPermission()) {
        setState(() => _error = 'Cần quyền micro để ghi âm TechTalk.');
        return;
      }
      await KoreanTtsPlayer.instance.stop();
      _audioBytes.clear();
      final stream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: 16000,
          numChannels: 1,
        ),
      );
      _audioSubscription = stream.listen(_onAudioChunk);
      _recordingTimer = Timer(const Duration(seconds: 29), _stopAndTranscribe);
      setState(() {
        _recording = true;
        _waveform = const [];
        _pendingVoice = null;
        _error = null;
      });
    } catch (error) {
      if (mounted) setState(() => _error = _errorMessage(error));
    }
  }

  void _onAudioChunk(Uint8List chunk) {
    _audioBytes.add(chunk);
    if (!mounted || !_recording) return;
    var total = 0.0;
    var count = 0;
    for (var index = 0; index + 1 < chunk.length; index += 2) {
      final raw = chunk[index] | (chunk[index + 1] << 8);
      final sample = raw >= 0x8000 ? raw - 0x10000 : raw;
      total += sample.abs();
      count++;
    }
    final value = count == 0 ? 0.0 : (total / count / 32767).clamp(0.0, 1.0);
    setState(() {
      _waveform = [..._waveform, value];
      if (_waveform.length > 48) _waveform = _waveform.sublist(1);
    });
  }

  Future<void> _stopAndTranscribe() async {
    if (!_recording) return;
    _recordingTimer?.cancel();
    await _audioSubscription?.cancel();
    await _recorder.stop();
    final bytes = Uint8List.fromList(_audioBytes.toBytes());
    if (!mounted) return;
    setState(() {
      _recording = false;
      _transcribing = true;
    });
    try {
      final session = _session;
      if (session == null) return;
      final transcription = await _service.transcribe(
        sessionId: session.id,
        bytes: bytes,
      );
      if (!mounted) return;
      setState(() {
        _pendingVoice = transcription;
        _input.text = transcription.transcript;
        _input.selection = TextSelection.collapsed(offset: _input.text.length);
      });
    } catch (error) {
      if (mounted) setState(() => _error = _errorMessage(error));
    } finally {
      if (mounted) setState(() => _transcribing = false);
    }
  }

  Future<void> _playTts(String text, {bool silent = false}) async {
    if (!silent && mounted) setState(() => _error = null);
    try {
      await KoreanTtsPlayer.instance.playDialogueOrStop(text);
    } on KoreanTtsException catch (error) {
      if (!silent && mounted) setState(() => _error = error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final strings = TechTalkStrings(settings.localeCode);
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_session == null) {
      return Scaffold(
        appBar: AppBar(),
        body: Center(
          child: Text(_error ?? 'Không thể bắt đầu phiên roleplay.'),
        ),
      );
    }
    final scenario = widget.scenario;
    return PopScope(
      canPop: _allowPop,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) unawaited(_requestLeave());
      },
      child: Scaffold(
        backgroundColor: AppTheme.background,
        appBar: AppBar(
          title: Text(strings.title),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: _requestLeave,
          ),
          actions: [
            IconButton(
              tooltip: strings.autoTts,
              onPressed: () =>
                  settings.setTechTalkAutoTts(!settings.techTalkAutoTts),
              icon: Icon(
                settings.techTalkAutoTts ? Icons.volume_up : Icons.volume_off,
              ),
            ),
            TextButton(
              onPressed: _ending || _sending ? null : _end,
              child: _ending
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(strings.end),
            ),
          ],
        ),
        body: SafeArea(
          child: Column(
            children: [
              _scenarioHeader(scenario, strings),
              Expanded(
                child: ListView(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(14),
                  children: [
                    ..._messages.map((message) => _message(message, strings)),
                    if (_hint != null) _hintCard(_hint!, strings),
                    if (_sending)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Text(
                          'Gemini · streaming',
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 9,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          _error!,
                          style: const TextStyle(color: Colors.redAccent),
                        ),
                      ),
                      if (_lastFailedText != null)
                        TextButton(
                          onPressed: _sending ? null : _retryFailed,
                          child: Text(strings.retry),
                        ),
                    ],
                  ),
                ),
              _composer(strings),
            ],
          ),
        ),
      ),
    );
  }

  Widget _scenarioHeader(
    TechTalkScenario scenario,
    TechTalkStrings strings,
  ) => Container(
    padding: const EdgeInsets.all(14),
    color: AppTheme.surface,
    child: Column(
      children: [
        Row(
          children: [
            _personaAvatar(scenario.persona),
            const SizedBox(width: 9),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    scenario.persona.name,
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
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
        const SizedBox(height: 6),
        ExpansionTile(
          tilePadding: EdgeInsets.zero,
          childrenPadding: const EdgeInsets.only(bottom: 6),
          title: Text(
            '🎯 ${strings.mission}: ${scenario.missionVi}',
            style: GoogleFonts.inter(
              fontSize: 12,
              color: AppTheme.textSecondary,
            ),
          ),
          children: [
            ...scenario.mission.objectives.map(
              (objective) => ListTile(
                dense: true,
                leading: const Icon(Icons.check_circle_outline, size: 16),
                title: Text(
                  strings.en && objective.en.isNotEmpty
                      ? objective.en
                      : objective.vi.isNotEmpty
                      ? objective.vi
                      : objective.ko,
                ),
              ),
            ),
            if (scenario.requiredVocabulary.isNotEmpty)
              Align(
                alignment: Alignment.centerLeft,
                child: Wrap(
                  spacing: 6,
                  children: scenario.requiredVocabulary
                      .map(
                        (word) => Chip(
                          label: Text(word),
                          visualDensity: VisualDensity.compact,
                        ),
                      )
                      .toList(),
                ),
              ),
          ],
        ),
      ],
    ),
  );

  Widget _message(RoleplayMessage message, TechTalkStrings strings) {
    final user = message.role == 'user';
    final evaluation = message.evaluation;
    final failed = message.generationStatus == 'failed';
    return Align(
      alignment: user ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        constraints: const BoxConstraints(maxWidth: 340),
        child: Column(
          crossAxisAlignment: user
              ? CrossAxisAlignment.end
              : CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (!user && message.content.isNotEmpty)
                  ValueListenableBuilder<KoreanTtsPlaybackState>(
                    valueListenable: KoreanTtsPlayer.instance.playbackState,
                    builder: (context, state, child) {
                      final active = state.matches(
                        message.content,
                        dialogue: true,
                      );
                      return IconButton(
                        visualDensity: VisualDensity.compact,
                        tooltip: active ? 'Dừng phát âm' : 'Phát âm',
                        onPressed: () => _playTts(message.content),
                        icon:
                            state.status == KoreanTtsPlaybackStatus.loading &&
                                active
                            ? const SizedBox.square(
                                dimension: 17,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                ),
                              )
                            : Icon(
                                active
                                    ? Icons.stop_circle_outlined
                                    : Icons.volume_up_outlined,
                                size: 17,
                              ),
                      );
                    },
                  ),
                Flexible(
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: failed
                          ? Colors.redAccent.withValues(alpha: .12)
                          : user
                          ? AppTheme.primary
                          : AppTheme.surface,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: message.content.isEmpty
                        ? const SizedBox(
                            width: 42,
                            child: LinearProgressIndicator(),
                          )
                        : Text(
                            message.content,
                            style: GoogleFonts.inter(
                              color: user ? Colors.black : AppTheme.textPrimary,
                              height: 1.45,
                            ),
                          ),
                  ),
                ),
              ],
            ),
            if (evaluation != null) _evaluation(evaluation, strings),
          ],
        ),
      ),
    );
  }

  Widget _personaAvatar(TechTalkPersona persona) => CircleAvatar(
    radius: 18,
    backgroundColor: AppTheme.background,
    child: persona.avatarUrl.isEmpty
        ? Text(persona.avatar, style: const TextStyle(fontSize: 23))
        : ClipOval(
            child: Image.network(
              persona.avatarUrl,
              width: 36,
              height: 36,
              fit: BoxFit.cover,
              errorBuilder: (_, _, _) =>
                  Text(persona.avatar, style: const TextStyle(fontSize: 23)),
            ),
          ),
  );

  Widget _evaluation(MessageEvaluation evaluation, TechTalkStrings strings) {
    if (evaluation.status == 'unavailable') {
      return Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          strings.evaluationUnavailable,
          style: GoogleFonts.jetBrainsMono(
            fontSize: 9,
            color: AppTheme.textSecondary,
          ),
        ),
      );
    }
    return Container(
      margin: const EdgeInsets.only(top: 4),
      constraints: const BoxConstraints(maxWidth: 320),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            'G ${evaluation.grammar} · V ${evaluation.vocabulary} · P ${evaluation.politeness}',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 10,
              color: AppTheme.textSecondary,
            ),
          ),
          if (evaluation.feedbackVi.isNotEmpty)
            Text(
              evaluation.feedbackVi,
              textAlign: TextAlign.right,
              style: GoogleFonts.inter(
                fontSize: 10,
                color: AppTheme.textSecondary,
              ),
            ),
          if (evaluation.corrections.isNotEmpty)
            ExpansionTile(
              tilePadding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              title: Text(
                '${strings.corrections} (${evaluation.corrections.length})',
                style: GoogleFonts.inter(fontSize: 11),
              ),
              children: evaluation.corrections
                  .map(
                    (correction) => ListTile(
                      dense: true,
                      title: Text(
                        '${correction.original} → ${correction.suggestion}',
                        style: GoogleFonts.inter(fontSize: 12),
                      ),
                      subtitle: Text(correction.noteVi),
                    ),
                  )
                  .toList(),
            ),
        ],
      ),
    );
  }

  Widget _hintCard(RoleplayHint hint, TechTalkStrings strings) => Card(
    color: AppTheme.secondary.withValues(alpha: .1),
    child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            strings.hint.toUpperCase(),
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

  Widget _composer(TechTalkStrings strings) => Padding(
    padding: const EdgeInsets.fromLTRB(12, 4, 12, 12),
    child: Column(
      children: [
        if (_recording)
          Container(
            height: 42,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                const Icon(Icons.fiber_manual_record, color: Colors.redAccent),
                const SizedBox(width: 8),
                Expanded(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: _waveform
                        .map(
                          (value) => Expanded(
                            child: Container(
                              margin: const EdgeInsets.symmetric(horizontal: 1),
                              height: 4 + value * 30,
                              color: AppTheme.primary,
                            ),
                          ),
                        )
                        .toList(),
                  ),
                ),
                const SizedBox(width: 8),
                Text(strings.recording),
              ],
            ),
          ),
        if (_transcribing)
          Padding(
            padding: const EdgeInsets.all(8),
            child: Row(
              children: [
                const SizedBox.square(
                  dimension: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                const SizedBox(width: 8),
                Text(strings.transcribing),
              ],
            ),
          ),
        if (_pendingVoice != null)
          Row(
            children: [
              const Icon(Icons.graphic_eq, size: 16),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  strings.voiceTranscript,
                  style: GoogleFonts.inter(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ),
              IconButton(
                onPressed: () => setState(() => _pendingVoice = null),
                icon: const Icon(Icons.close, size: 16),
              ),
            ],
          ),
        Row(
          children: [
            IconButton(
              onPressed: _hintLoading ? null : _hintRequest,
              tooltip: strings.hint,
              icon: _hintLoading
                  ? const CircularProgressIndicator(strokeWidth: 2)
                  : const Icon(
                      Icons.lightbulb_outline,
                      color: AppTheme.secondary,
                    ),
            ),
            Expanded(
              child: TextField(
                controller: _input,
                enabled: !_recording && !_transcribing,
                onSubmitted: (_) => _send(),
                decoration: InputDecoration(
                  hintText: strings.inputHint,
                  filled: true,
                  fillColor: AppTheme.surface,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 4),
            IconButton(
              onPressed: _sending || _transcribing ? null : _toggleRecording,
              tooltip: _recording ? strings.recording : 'Microphone',
              icon: Icon(
                _recording ? Icons.stop_circle : Icons.mic,
                color: _recording ? Colors.redAccent : AppTheme.secondary,
              ),
            ),
            IconButton(
              onPressed: _sending || _recording || _transcribing ? null : _send,
              icon: _sending
                  ? const CircularProgressIndicator(strokeWidth: 2)
                  : const Icon(Icons.send, color: AppTheme.primary),
            ),
          ],
        ),
      ],
    ),
  );

  RoleplayMessage? _findMessage(String id) {
    for (final message in _messages) {
      if (message.id == id) return message;
    }
    return null;
  }

  void _replaceMessage(String id, RoleplayMessage replacement) {
    if (!mounted) return;
    setState(() {
      _messages = _messages
          .map((message) => message.id == id ? replacement : message)
          .toList();
    });
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  String _clientTurnId() =>
      '${DateTime.now().microsecondsSinceEpoch}-${_random.nextInt(1 << 32)}';

  String _errorMessage(Object error) =>
      error.toString().replaceFirst('Exception: ', '');
}

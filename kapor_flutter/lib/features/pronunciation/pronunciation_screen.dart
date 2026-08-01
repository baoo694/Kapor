import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:just_audio/just_audio.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../../core/audio/korean_tts_player.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import 'data/pronunciation_service.dart';
import 'widgets/wrong_sentence_alert.dart';

class PronunciationScreen extends StatefulWidget {
  const PronunciationScreen({super.key, required this.exercise});
  final PronunciationExercise exercise;
  @override
  State<PronunciationScreen> createState() => _PronunciationScreenState();
}

class _PronunciationScreenState extends State<PronunciationScreen> {
  final _recorder = AudioRecorder();
  final _player = AudioPlayer();
  final _service = PronunciationService();
  final _bytes = BytesBuilder(copy: false);
  StreamSubscription<Uint8List>? _subscription;
  StreamSubscription<Duration>? _positionSubscription;
  Timer? _recordingLimitTimer;
  bool _recording = false;
  bool _evaluating = false;
  String? _playingKey;
  PronunciationResult? _result;
  List<double> _liveWaveform = const [];
  List<PronunciationAttemptSummary> _history = const [];
  String? _error;
  Duration _playbackPosition = Duration.zero;

  @override
  void initState() {
    super.initState();
    _positionSubscription = _player.positionStream.listen((position) {
      if (mounted && _playingKey != null) {
        setState(() => _playbackPosition = position);
      }
    });
    unawaited(_loadHistory());
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _positionSubscription?.cancel();
    _recordingLimitTimer?.cancel();
    _recorder.dispose();
    _player.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    try {
      final history = await _service.history(widget.exercise.id);
      if (mounted) setState(() => _history = history);
    } catch (_) {
      // The lab remains usable if history cannot be loaded.
    }
  }

  Future<void> _record() async {
    if (_recording) {
      await _subscription?.cancel();
      _recordingLimitTimer?.cancel();
      await _recorder.stop();
      if (mounted) setState(() => _recording = false);
      return;
    }
    try {
      if (!await _recorder.hasPermission()) {
        setState(() => _error = 'Cần quyền micro để ghi âm.');
        return;
      }
      await _stopPlayback();
      _bytes.clear();
      final stream = await _recorder.startStream(
        // Keep this PCM contract aligned with PcmWavConverter on the backend:
        // signed 16-bit, 16 kHz, one channel.
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: 16000,
          numChannels: 1,
        ),
      );
      _subscription = stream.listen(_onAudioChunk);
      _recordingLimitTimer = Timer(const Duration(seconds: 29), () async {
        if (!_recording) return;
        await _record();
        if (mounted) {
          setState(
            () => _error =
                'Bản ghi đã tự dừng sau 29 giây để Azure đánh giá chính xác.',
          );
        }
      });
      setState(() {
        _recording = true;
        _result = null;
        _liveWaveform = const [];
        _error = null;
      });
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể bắt đầu ghi âm.');
    }
  }

  void _onAudioChunk(Uint8List chunk) {
    _bytes.add(chunk);
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
      _liveWaveform = [..._liveWaveform, value];
      if (_liveWaveform.length > 64) _liveWaveform = _liveWaveform.sublist(1);
    });
  }

  Future<void> _evaluate() async {
    if (_recording) await _record();
    final data = Uint8List.fromList(_bytes.toBytes());
    if (data.isEmpty) {
      setState(() => _error = 'Hãy ghi âm trước khi đánh giá.');
      return;
    }
    setState(() {
      _evaluating = true;
      _error = null;
    });
    try {
      final result = await _service.evaluate(
        exerciseId: widget.exercise.id,
        bytes: data,
      );
      if (!mounted) return;
      setState(() {
        _result = result;
        _history = [result.toHistory(), ..._history];
      });
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _evaluating = false);
    }
  }

  Future<void> _playReference(PronunciationSentence sentence) async {
    if (sentence.audioUrl.isEmpty) {
      try {
        await KoreanTtsPlayer.instance.playOrStop(sentence.text);
      } on KoreanTtsException catch (error) {
        if (mounted) setState(() => _error = error.message);
      }
      return;
    }
    final uri = Uri.tryParse(sentence.audioUrl);
    if (uri == null) {
      setState(() => _error = 'Đường dẫn audio mẫu không hợp lệ.');
      return;
    }
    final source = uri.hasScheme
        ? uri
        : Uri.parse(ApiClient.baseUrl).resolveUri(uri);
    await _playUrl(source, 'reference');
  }

  Future<void> _playAttempt(PronunciationAttemptSummary attempt) async {
    if (attempt.attemptAudioUrl.isEmpty) {
      setState(() => _error = 'Bản ghi này đã hết hạn lưu trữ.');
      return;
    }
    final key = 'attempt-${attempt.id}';
    if (_playingKey == key) {
      await _stopPlayback();
      return;
    }
    try {
      final audio = await _service.attemptAudio(attempt.attemptAudioUrl);
      final directory = await getTemporaryDirectory();
      final file = File(
        '${directory.path}/kapor-pronunciation-${attempt.id}.wav',
      );
      await file.writeAsBytes(audio, flush: true);
      await _playFile(file, key);
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    }
  }

  Future<void> _playUrl(Uri source, String key) async {
    if (_playingKey == key) {
      await _stopPlayback();
      return;
    }
    try {
      await _player.setUrl(source.toString());
      await _beginPlayback(key);
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể phát audio mẫu.');
    }
  }

  Future<void> _playFile(File file, String key) async {
    await _player.setFilePath(file.path);
    await _beginPlayback(key);
  }

  Future<void> _beginPlayback(String key) async {
    _playbackPosition = Duration.zero;
    if (mounted) setState(() => _playingKey = key);
    try {
      await _player.play();
    } finally {
      if (mounted && _playingKey == key) setState(() => _playingKey = null);
    }
  }

  Future<void> _stopPlayback() async {
    await _player.stop();
    if (mounted) {
      setState(() {
        _playingKey = null;
        _playbackPosition = Duration.zero;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final sentence = widget.exercise.sentences.isEmpty
        ? const PronunciationSentence(
            text: '',
            translationVi: '',
            audioUrl: '',
            waveform: [],
          )
        : widget.exercise.sentences.first;
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: context.pop,
        ),
        title: const Text('Pronunciation Lab'),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _referenceCard(sentence),
            const SizedBox(height: 12),
            _recordingCard(),
            if (_error != null) _errorMessage(),
            if (_result != null) _resultCard(_result!, sentence),
            if (_history.isNotEmpty) _historySection(),
          ],
        ),
      ),
    );
  }

  Widget _referenceCard(PronunciationSentence sentence) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _label('CÂU MẪU'),
          const SizedBox(height: 8),
          Text(
            sentence.text,
            style: GoogleFonts.outfit(
              fontSize: 21,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            sentence.translationVi,
            style: GoogleFonts.inter(
              fontSize: 12,
              color: AppTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 15),
          _wave(sentence.waveform, AppTheme.primary),
          const SizedBox(height: 8),
          ValueListenableBuilder<String?>(
            valueListenable: KoreanTtsPlayer.instance.activeText,
            builder: (context, activeText, child) {
              final ttsPlaying = activeText == sentence.text;
              final isPlaying = _playingKey == 'reference' || ttsPlaying;
              return OutlinedButton.icon(
                onPressed: sentence.text.isEmpty
                    ? null
                    : () => _playReference(sentence),
                icon: Icon(isPlaying ? Icons.stop : Icons.play_arrow),
                label: Text(
                  sentence.audioUrl.isEmpty
                      ? 'Nghe mẫu bằng TTS'
                      : 'Nghe audio mẫu',
                ),
              );
            },
          ),
        ],
      ),
    ),
  );

  Widget _recordingCard() => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _label('BẢN GHI CỦA BẠN'),
          const SizedBox(height: 12),
          _wave(
            _result?.userWaveform ?? _liveWaveform,
            _recording ? Colors.redAccent : AppTheme.secondary,
          ),
          const SizedBox(height: 14),
          ElevatedButton.icon(
            onPressed: _evaluating ? null : _record,
            icon: Icon(_recording ? Icons.stop : Icons.mic),
            label: Text(_recording ? 'Dừng ghi âm' : 'Bắt đầu ghi âm'),
            style: ElevatedButton.styleFrom(
              backgroundColor: _recording ? Colors.redAccent : AppTheme.primary,
              foregroundColor: Colors.black,
            ),
          ),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: _evaluating ? null : _evaluate,
            child: _evaluating
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Đánh giá phát âm'),
          ),
          if (_result?.attemptAudioUrl.isNotEmpty ?? false)
            TextButton.icon(
              onPressed: () => _playAttempt(_result!.toHistory()),
              icon: Icon(
                _playingKey == 'attempt-${_result!.attemptId}'
                    ? Icons.stop
                    : Icons.play_arrow,
              ),
              label: const Text('Nghe lại bản ghi'),
            ),
        ],
      ),
    ),
  );

  Widget _errorMessage() => Padding(
    padding: const EdgeInsets.only(top: 12),
    child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
  );

  Widget _resultCard(
    PronunciationResult result,
    PronunciationSentence sentence,
  ) {
    if (result.isWrongSentence) {
      final transcript = result.transcriptionText.trim().isNotEmpty
          ? result.transcriptionText
          : result.transcript?.text ?? '';
      return WrongSentenceAlert(
        expectedText: sentence.text,
        transcriptText: transcript,
        message: result.message,
        onRetry: _evaluating ? null : () => unawaited(_record()),
      );
    }
    final focusWords = _focusWords(result.feedback);
    final isComplete = result.status == 'completed';
    return Card(
      margin: const EdgeInsets.only(top: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _label(isComplete ? 'KẾT QUẢ CỦA BẠN' : 'TRẠNG THÁI'),
            const SizedBox(height: 8),
            Text(
              isComplete
                  ? _scoreSummary(result.scores?.overall)
                  : result.message,
              style: GoogleFonts.inter(
                color: AppTheme.textPrimary,
                fontSize: isComplete ? 16 : 14,
                fontWeight: isComplete ? FontWeight.w600 : FontWeight.w400,
                height: 1.45,
              ),
            ),
            if (result.scores != null) ...[
              const SizedBox(height: 16),
              _scores(result.scores!),
            ],
            if (isComplete) ...[
              const SizedBox(height: 20),
              _label('CẦN LUYỆN THÊM'),
              const SizedBox(height: 8),
              if (focusWords.isEmpty)
                Text(
                  'Không có từ nào cần chú ý thêm trong lần đọc này.',
                  style: GoogleFonts.inter(
                    fontSize: 13,
                    color: AppTheme.textSecondary,
                    height: 1.4,
                  ),
                )
              else
                _focusFeedback(focusWords),
              const SizedBox(height: 14),
              _label('GỢI Ý'),
              const SizedBox(height: 6),
              Text(
                _practiceTip(result.analysis, focusWords),
                style: GoogleFonts.inter(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                  height: 1.45,
                ),
              ),
            ],
            if (_hasDetails(result)) ...[
              const SizedBox(height: 8),
              _details(result),
            ],
          ],
        ),
      ),
    );
  }

  String _scoreSummary(int? score) {
    if (score == null) return 'Đã nhận kết quả đánh giá.';
    if (score >= 85) return 'Phát âm tốt. Hãy giữ nhịp đọc tự nhiên.';
    if (score >= 65) return 'Khá tốt. Luyện thêm các từ bên dưới để rõ hơn.';
    return 'Hãy luyện lại câu này, bắt đầu với các từ bên dưới.';
  }

  List<PronunciationWordFeedback> _uniqueFeedback(
    List<PronunciationWordFeedback> words,
  ) {
    final lowestScoreByWord = <String, PronunciationWordFeedback>{};
    for (final word in words) {
      final key = word.text.trim();
      if (key.isEmpty) continue;
      final current = lowestScoreByWord[key];
      if (current == null || word.score < current.score) {
        lowestScoreByWord[key] = word;
      }
    }
    return lowestScoreByWord.values.toList()
      ..sort((left, right) => left.score.compareTo(right.score));
  }

  List<PronunciationWordFeedback> _focusWords(
    List<PronunciationWordFeedback> words,
  ) => _uniqueFeedback(words)
      .where(
        (word) =>
            word.score < 85 || !word.errorType.toLowerCase().contains('none'),
      )
      .take(3)
      .toList();

  String _practiceTip(
    PronunciationAnalysis? analysis,
    List<PronunciationWordFeedback> focusWords,
  ) {
    final tip = analysis?.interpretations
        .map((item) => item.practiceTipVi.trim())
        .where((item) => item.isNotEmpty)
        .cast<String>()
        .firstOrNull;
    if (tip != null) return tip;
    if (focusWords.isEmpty) {
      return 'Nghe lại câu mẫu rồi đọc lại một lần với tốc độ tự nhiên.';
    }
    return 'Nghe câu mẫu, đọc chậm các từ trên rồi ghép lại thành cả câu.';
  }

  bool _hasDetails(PronunciationResult result) =>
      result.transcriptionText.isNotEmpty ||
      (result.transcript?.words.isNotEmpty ?? false) ||
      result.feedback.isNotEmpty;

  Widget _focusFeedback(List<PronunciationWordFeedback> words) => Wrap(
    spacing: 6,
    runSpacing: 6,
    children: words
        .map(
          (word) => Tooltip(
            message: 'Điểm chính xác ${word.score}/100',
            child: Semantics(
              label: '${word.text}, ${word.score} trên 100',
              child: Chip(
                label: Text('${word.text} · ${word.score}'),
                backgroundColor: _feedbackColor(
                  word.score,
                ).withValues(alpha: .18),
                side: BorderSide(
                  color: _feedbackColor(word.score).withValues(alpha: .65),
                ),
                labelStyle: GoogleFonts.inter(
                  color: _feedbackColor(word.score),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        )
        .toList(),
  );

  Widget _details(PronunciationResult result) => ExpansionTile(
    tilePadding: EdgeInsets.zero,
    childrenPadding: const EdgeInsets.only(bottom: 4),
    iconColor: AppTheme.textSecondary,
    collapsedIconColor: AppTheme.textSecondary,
    title: Text(
      'Xem transcript và điểm chi tiết',
      style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textSecondary),
    ),
    children: [
      if (result.transcriptionText.isNotEmpty) ...[
        Align(
          alignment: Alignment.centerLeft,
          child: Text(
            'Bạn đã đọc: ${result.transcriptionText}',
            style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textPrimary),
          ),
        ),
        const SizedBox(height: 12),
      ],
      if (result.transcript?.words.isNotEmpty ?? false) ...[
        Align(alignment: Alignment.centerLeft, child: _label('MỐC THỜI GIAN')),
        const SizedBox(height: 8),
        _timeline(result.transcript!),
        const SizedBox(height: 14),
      ],
      if (result.feedback.isNotEmpty) ...[
        Align(alignment: Alignment.centerLeft, child: _label('ĐIỂM TỪNG ÂM')),
        const SizedBox(height: 8),
        _feedback(_uniqueFeedback(result.feedback)),
      ],
    ],
  );

  Widget _historySection() => Padding(
    padding: const EdgeInsets.only(top: 20),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _label('LỊCH SỬ GẦN ĐÂY'),
        const SizedBox(height: 8),
        ..._history.take(5).map((attempt) {
          final wrongSentence = attempt.isWrongSentence;
          return Card(
            child: ListTile(
              leading: Icon(
                wrongSentence
                    ? Icons.report_gmailerrorred_outlined
                    : attempt.status == 'completed'
                    ? Icons.check_circle_outline
                    : Icons.error_outline,
                color: wrongSentence
                    ? Colors.redAccent
                    : attempt.status == 'completed'
                    ? AppTheme.secondary
                    : AppTheme.textSecondary,
              ),
              title: Text(
                wrongSentence
                    ? 'Đọc khác câu mẫu'
                    : attempt.scores == null
                    ? 'Đang chờ kết quả'
                    : 'Tổng điểm ${attempt.scores!.overall}/100',
                style: GoogleFonts.outfit(fontWeight: FontWeight.w600),
              ),
              isThreeLine:
                  wrongSentence && attempt.transcriptionText.isNotEmpty,
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (wrongSentence && attempt.transcriptionText.isNotEmpty)
                    Text(
                      'WhisperX: ${attempt.transcriptionText}',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  Text(
                    attempt.attemptedAt == null
                        ? ''
                        : _timeLabel(attempt.attemptedAt!),
                    style: GoogleFonts.inter(
                      fontSize: 12,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ),
              trailing: IconButton(
                tooltip: attempt.attemptAudioUrl.isEmpty
                    ? 'Audio đã hết hạn'
                    : 'Nghe lại bản ghi',
                onPressed: attempt.attemptAudioUrl.isEmpty
                    ? null
                    : () => _playAttempt(attempt),
                icon: Icon(
                  _playingKey == 'attempt-${attempt.id}'
                      ? Icons.stop
                      : Icons.play_arrow,
                ),
              ),
            ),
          );
        }),
      ],
    ),
  );

  Widget _scores(PronunciationScores scores) => Row(
    children: [
      _score('Phát âm', scores.overall, AppTheme.primary),
      _score('Chính xác', scores.accuracy, AppTheme.secondary),
      _score('Lưu loát', scores.fluency, Colors.lightBlueAccent),
      _score('Đủ từ', scores.completeness, Colors.orangeAccent),
    ],
  );

  Widget _score(String label, int value, Color color) => Expanded(
    child: Column(
      children: [
        Text(
          '$value',
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.w800,
            fontSize: 20,
            color: color,
          ),
        ),
        Text(
          label,
          textAlign: TextAlign.center,
          style: GoogleFonts.inter(fontSize: 10, color: AppTheme.textSecondary),
        ),
      ],
    ),
  );

  Widget _feedback(List<PronunciationWordFeedback> words) => Wrap(
    spacing: 6,
    runSpacing: 6,
    children: words
        .map(
          (word) => Tooltip(
            message: word.phonemeDetail.isEmpty
                ? 'Azure · ${word.errorType}'
                : word.phonemeDetail,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Chip(
                  label: Text('${word.text} ${word.score}'),
                  backgroundColor: _feedbackColor(
                    word.score,
                  ).withValues(alpha: .18),
                  side: BorderSide(
                    color: _feedbackColor(word.score).withValues(alpha: .65),
                  ),
                  labelStyle: GoogleFonts.inter(
                    color: _feedbackColor(word.score),
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (word.phonemes.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(left: 4),
                    child: Text(
                      word.phonemes
                          .map(
                            (phoneme) =>
                                'Âm ${phoneme.index + 1}: ${phoneme.score}',
                          )
                          .join(' · '),
                      style: GoogleFonts.inter(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        )
        .toList(),
  );

  Widget _timeline(PronunciationTranscript transcript) => Wrap(
    spacing: 6,
    runSpacing: 6,
    children: transcript.words.map((word) {
      final start = word.startSeconds;
      final end = word.endSeconds;
      final active =
          start != null &&
          end != null &&
          _playbackPosition.inMilliseconds / 1000 >= start &&
          _playbackPosition.inMilliseconds / 1000 <= end;
      final timing = start == null || end == null
          ? 'không căn chỉnh'
          : '${start.toStringAsFixed(1)}–${end.toStringAsFixed(1)}s';
      return Tooltip(
        message: timing,
        child: Chip(
          label: Text(word.text),
          backgroundColor: (active ? AppTheme.primary : AppTheme.surface)
              .withValues(alpha: active ? .42 : .75),
          labelStyle: GoogleFonts.inter(
            color: active ? Colors.black : AppTheme.textPrimary,
            fontWeight: active ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      );
    }).toList(),
  );

  Color _feedbackColor(int score) => score >= 85
      ? Colors.lightGreenAccent
      : score >= 65
      ? Colors.orangeAccent
      : Colors.redAccent;

  Widget _label(String value) => Text(
    value,
    style: GoogleFonts.jetBrainsMono(
      fontSize: 10,
      color: AppTheme.textSecondary,
      letterSpacing: 1,
    ),
  );

  Widget _wave(List<double> values, Color color) {
    final bars = values.isEmpty ? List<double>.filled(32, .12) : values;
    return SizedBox(
      height: 52,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: bars
            .take(64)
            .map(
              (value) => Expanded(
                child: Container(
                  height: 5 + value.clamp(0, 1) * 44,
                  margin: const EdgeInsets.symmetric(horizontal: 1),
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: .75),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  String _timeLabel(DateTime time) {
    final local = time.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')} · ${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
  }
}

import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:record/record.dart';

import '../../core/theme/app_theme.dart';
import 'data/pronunciation_service.dart';

class PronunciationScreen extends StatefulWidget {
  const PronunciationScreen({super.key, required this.exercise});
  final PronunciationExercise exercise;
  @override
  State<PronunciationScreen> createState() => _PronunciationScreenState();
}

class _PronunciationScreenState extends State<PronunciationScreen> {
  final _recorder = AudioRecorder();
  final _service = PronunciationService();
  final _bytes = BytesBuilder(copy: false);
  StreamSubscription<Uint8List>? _subscription;
  bool _recording = false;
  bool _evaluating = false;
  PronunciationResult? _result;
  String? _error;

  @override
  void dispose() {
    _subscription?.cancel();
    _recorder.dispose();
    super.dispose();
  }

  Future<void> _record() async {
    if (_recording) {
      await _subscription?.cancel();
      await _recorder.stop();
      if (mounted) setState(() => _recording = false);
      return;
    }
    try {
      if (!await _recorder.hasPermission()) {
        setState(() => _error = 'Cần quyền micro để ghi âm.');
        return;
      }
      _bytes.clear();
      final stream = await _recorder.startStream(
        const RecordConfig(encoder: AudioEncoder.pcm16bits, sampleRate: 16000),
      );
      _subscription = stream.listen(_bytes.add);
      setState(() {
        _recording = true;
        _result = null;
        _error = null;
      });
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể bắt đầu ghi âm.');
    }
  }

  Future<void> _evaluate() async {
    if (_recording) await _record();
    final data = _bytes.takeBytes();
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
      if (mounted) setState(() => _result = result);
    } catch (error) {
      if (mounted)
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
    } finally {
      if (mounted) setState(() => _evaluating = false);
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
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'CÂU MẪU',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                      ),
                    ),
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
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'BẢN GHI CỦA BẠN',
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    _wave(
                      _result?.userWaveform ?? const [],
                      _recording ? Colors.redAccent : AppTheme.secondary,
                    ),
                    const SizedBox(height: 14),
                    ElevatedButton.icon(
                      onPressed: _evaluating ? null : _record,
                      icon: Icon(_recording ? Icons.stop : Icons.mic),
                      label: Text(
                        _recording ? 'Dừng ghi âm' : 'Bắt đầu ghi âm',
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _recording
                            ? Colors.redAccent
                            : AppTheme.primary,
                        foregroundColor: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 8),
                    OutlinedButton(
                      onPressed: _evaluating ? null : _evaluate,
                      child: _evaluating
                          ? const CircularProgressIndicator()
                          : const Text('Đánh giá phát âm'),
                    ),
                  ],
                ),
              ),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(
                  _error!,
                  style: const TextStyle(color: Colors.redAccent),
                ),
              ),
            if (_result != null)
              Card(
                margin: const EdgeInsets.only(top: 12),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _result!.status == 'pending_provider'
                            ? 'ĐÃ GỬI BẢN GHI'
                            : 'KẾT QUẢ',
                        style: GoogleFonts.jetBrainsMono(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _result!.message,
                        style: GoogleFonts.inter(
                          color: AppTheme.textPrimary,
                          height: 1.5,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

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
                    color: color.withOpacity(.75),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
            )
            .toList(),
      ),
    );
  }
}

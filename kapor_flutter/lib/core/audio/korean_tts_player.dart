import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import 'package:path_provider/path_provider.dart';

import '../network/api_client.dart';

class KoreanTtsException implements Exception {
  const KoreanTtsException(this.message);

  final String message;

  @override
  String toString() => message;
}

enum KoreanTtsPlaybackStatus { idle, loading, playing }

@immutable
class KoreanTtsPlaybackState {
  const KoreanTtsPlaybackState({
    required this.status,
    this.text,
    this.dialogue = false,
  });

  const KoreanTtsPlaybackState.idle()
    : status = KoreanTtsPlaybackStatus.idle,
      text = null,
      dialogue = false;

  final KoreanTtsPlaybackStatus status;
  final String? text;
  final bool dialogue;

  bool matches(String value, {required bool dialogue}) =>
      status != KoreanTtsPlaybackStatus.idle &&
      text == value &&
      this.dialogue == dialogue;
}

/// App-wide audio player for Gemini-generated Korean vocabulary pronunciation.
///
/// The server owns the Gemini API key and caches generated WAV files. This
/// player keeps a second, temporary cache on the device for instant replays.
class KoreanTtsPlayer {
  KoreanTtsPlayer._();

  static final KoreanTtsPlayer instance = KoreanTtsPlayer._();

  final Dio _dio = ApiClient().dio;
  final AudioPlayer _player = AudioPlayer();
  final ValueNotifier<String?> activeText = ValueNotifier<String?>(null);
  final ValueNotifier<KoreanTtsPlaybackState> playbackState =
      ValueNotifier<KoreanTtsPlaybackState>(
        const KoreanTtsPlaybackState.idle(),
      );
  final Map<String, File> _files = {};
  CancelToken? _downloadCancelToken;
  int _operation = 0;

  Future<void> playOrStop(String rawText) => _playOrStop(rawText, false);

  Future<void> playDialogueOrStop(String rawText) => _playOrStop(rawText, true);

  Future<void> _playOrStop(String rawText, bool dialogue) async {
    final text = rawText.trim();
    if (text.isEmpty) {
      throw const KoreanTtsException('Không có từ tiếng Hàn để phát âm.');
    }

    if (playbackState.value.matches(text, dialogue: dialogue)) {
      await stop();
      return;
    }

    final operation = ++_operation;
    _downloadCancelToken?.cancel();
    await _player.stop();
    if (operation != _operation) return;
    _setState(
      KoreanTtsPlaybackState(
        status: KoreanTtsPlaybackStatus.loading,
        text: text,
        dialogue: dialogue,
      ),
    );
    try {
      final cancelToken = CancelToken();
      _downloadCancelToken = cancelToken;
      final audioFile = await _audioFileFor(
        text,
        dialogue,
        cancelToken: cancelToken,
      );
      if (operation != _operation) return;

      await _player.setFilePath(audioFile.path);
      if (operation != _operation) return;
      _setState(
        KoreanTtsPlaybackState(
          status: KoreanTtsPlaybackStatus.playing,
          text: text,
          dialogue: dialogue,
        ),
      );
      // Await playback so decoder/device errors reach the caller instead of
      // being swallowed by an unawaited Future.
      await _player.play();
    } catch (error) {
      if (operation != _operation ||
          (error is DioException && CancelToken.isCancel(error))) {
        return;
      }
      if (error is KoreanTtsException) rethrow;
      throw const KoreanTtsException(
        'Không thể phát audio trên thiết bị. Vui lòng thử lại.',
      );
    } finally {
      if (operation == _operation) {
        _downloadCancelToken = null;
        _setState(const KoreanTtsPlaybackState.idle());
      }
    }
  }

  Future<void> stop() async {
    _operation++;
    _downloadCancelToken?.cancel();
    _downloadCancelToken = null;
    _setState(const KoreanTtsPlaybackState.idle());
    await _player.stop();
  }

  void _setState(KoreanTtsPlaybackState state) {
    playbackState.value = state;
    activeText.value = state.text;
  }

  Future<File> _audioFileFor(
    String text,
    bool dialogue, {
    required CancelToken cancelToken,
  }) async {
    final memoryKey = '${dialogue ? 'dialogue' : 'vocabulary'}|$text';
    final inMemoryFile = _files[memoryKey];
    if (inMemoryFile != null && await _isPlayableWav(inMemoryFile)) {
      return inMemoryFile;
    }

    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/${_cacheFileName(text, dialogue)}');
    if (await _isPlayableWav(file)) {
      _files[memoryKey] = file;
      return file;
    }
    if (await file.exists()) await file.delete();

    try {
      final response = await _dio.post<List<int>>(
        dialogue ? '/tts/korean/dialogue' : '/tts/korean',
        data: {'text': text},
        options: Options(
          responseType: ResponseType.bytes,
          // Gemini speech generation can legitimately take longer than the
          // API client's 15-second JSON timeout, especially on a cold cache.
          receiveTimeout: const Duration(seconds: 45),
          // The endpoint returns WAV on success but a JSON API error on
          // failure. Advertise both so Spring can send the useful error body.
          headers: {'Accept': 'audio/wav, application/json'},
        ),
        cancelToken: cancelToken,
      );
      final audio = response.data;
      if (audio == null || !_hasWavHeader(audio)) {
        throw const KoreanTtsException(
          'Máy chủ trả về dữ liệu phát âm không hợp lệ.',
        );
      }
      await file.writeAsBytes(audio, flush: true);
      _files[memoryKey] = file;
      return file;
    } on DioException catch (error) {
      throw KoreanTtsException(_messageFromError(error));
    }
  }

  Future<bool> _isPlayableWav(File file) async {
    if (!await file.exists() || await file.length() <= 44) return false;
    try {
      return _hasWavHeader(
        await file
            .openRead(0, 12)
            .fold<List<int>>(<int>[], (bytes, chunk) => bytes..addAll(chunk)),
      );
    } on FileSystemException {
      return false;
    }
  }

  bool _hasWavHeader(List<int> bytes) =>
      bytes.length >= 12 &&
      ascii.decode(bytes.sublist(0, 4), allowInvalid: true) == 'RIFF' &&
      ascii.decode(bytes.sublist(8, 12), allowInvalid: true) == 'WAVE';

  String _cacheFileName(String text, bool dialogue) {
    var hash = 0x811c9dc5;
    for (final codeUnit in text.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xffffffff;
    }
    return 'kapor-tts-${dialogue ? 'dialogue' : 'word'}-${hash.toRadixString(16)}-${text.length}.wav';
  }

  String _messageFromError(DioException error) {
    final data = error.response?.data;
    Map<String, dynamic>? body;
    if (data is Map) {
      body = Map<String, dynamic>.from(data);
    } else if (data is List<int>) {
      try {
        final decoded = jsonDecode(utf8.decode(data));
        if (decoded is Map) body = Map<String, dynamic>.from(decoded);
      } on FormatException {
        // Fall through to the status-specific fallback below.
      }
    }

    final message = body?['message']?.toString();
    if (message != null && message.isNotEmpty) return message;
    if (error.response?.statusCode == 429) {
      return 'Dịch vụ phát âm đang bận. Vui lòng thử lại sau.';
    }
    if (error.type == DioExceptionType.receiveTimeout ||
        error.type == DioExceptionType.connectionTimeout) {
      return 'Tạo giọng nói mất quá nhiều thời gian. Vui lòng thử lại.';
    }
    return 'Không thể tạo phát âm. Kiểm tra kết nối rồi thử lại.';
  }
}

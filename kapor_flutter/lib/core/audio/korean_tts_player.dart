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

/// App-wide audio player for Gemini-generated Korean vocabulary pronunciation.
///
/// The server owns the Gemini API key and caches generated WAV files. This
/// player keeps a second, temporary cache on the device for instant replays.
class KoreanTtsPlayer {
  KoreanTtsPlayer._() {
    _player.playerStateStream.listen((state) {
      if (state.processingState == ProcessingState.completed ||
          state.processingState == ProcessingState.idle) {
        activeText.value = null;
      }
    });
  }

  static final KoreanTtsPlayer instance = KoreanTtsPlayer._();

  final Dio _dio = ApiClient().dio;
  final AudioPlayer _player = AudioPlayer();
  final ValueNotifier<String?> activeText = ValueNotifier<String?>(null);
  final Map<String, File> _files = {};

  Future<void> playOrStop(String rawText) async {
    final text = rawText.trim();
    if (text.isEmpty) {
      throw const KoreanTtsException('Không có từ tiếng Hàn để phát âm.');
    }

    if (activeText.value == text) {
      await stop();
      return;
    }

    await _player.stop();
    activeText.value = text;
    try {
      final audioFile = await _audioFileFor(text);
      if (activeText.value != text) return;

      await _player.setFilePath(audioFile.path);
      if (activeText.value != text) return;
      unawaited(
        _player.play().catchError((_) {
          if (activeText.value == text) activeText.value = null;
        }),
      );
    } catch (error) {
      if (activeText.value == text) activeText.value = null;
      if (error is KoreanTtsException) rethrow;
      throw KoreanTtsException('Không thể phát âm từ này. Vui lòng thử lại.');
    }
  }

  Future<void> stop() async {
    activeText.value = null;
    await _player.stop();
  }

  Future<File> _audioFileFor(String text) async {
    final inMemoryFile = _files[text];
    if (inMemoryFile != null && await inMemoryFile.exists()) {
      return inMemoryFile;
    }

    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/${_cacheFileName(text)}');
    if (await file.exists() && await file.length() > 44) {
      _files[text] = file;
      return file;
    }

    try {
      final response = await _dio.post<List<int>>(
        '/tts/korean',
        data: {'text': text},
        options: Options(
          responseType: ResponseType.bytes,
          // The endpoint returns WAV on success but a JSON API error on
          // failure. Advertise both so Spring can send the useful error body.
          headers: {'Accept': 'audio/wav, application/json'},
        ),
      );
      final audio = response.data;
      if (audio == null || audio.length <= 44) {
        throw const KoreanTtsException('Máy chủ không trả về audio phát âm.');
      }
      await file.writeAsBytes(audio, flush: true);
      _files[text] = file;
      return file;
    } on DioException catch (error) {
      throw KoreanTtsException(_messageFromError(error));
    }
  }

  String _cacheFileName(String text) {
    var hash = 0x811c9dc5;
    for (final codeUnit in text.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xffffffff;
    }
    return 'kapor-tts-${hash.toRadixString(16)}-${text.length}.wav';
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
    return 'Không thể tạo phát âm. Kiểm tra kết nối rồi thử lại.';
  }
}

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:youtube_player_iframe/youtube_player_iframe.dart';

import '../../core/theme/app_theme.dart';
import 'data/video_service.dart';

class VideoScreen extends StatefulWidget {
  const VideoScreen({super.key});
  @override
  State<VideoScreen> createState() => _VideoScreenState();
}

class _VideoScreenState extends State<VideoScreen> {
  static const _videoTeal = Color(0xFF2DD4BF);
  static const _videoAmber = Color(0xFFFBBF24);

  final _service = VideoService();
  late Future<List<LearningVideo>> _videosFuture;
  LearningVideo? _video;
  YoutubePlayerController? _player;
  StreamSubscription<YoutubeVideoState>? _positionSubscription;
  StreamSubscription<YoutubePlayerValue>? _playerValueSubscription;
  double _position = 0;
  double _durationSeconds = 0;
  double _playbackRate = 1;
  bool _isPlaying = false;
  final Set<String> _completedQuizzes = {};
  String? _error;

  @override
  void initState() {
    super.initState();
    _videosFuture = _service.getVideos();
  }

  @override
  void dispose() {
    _positionSubscription?.cancel();
    _playerValueSubscription?.cancel();
    _player?.close();
    super.dispose();
  }

  void _select(LearningVideo video) {
    if (video.youtubeVideoId.trim().isEmpty) {
      setState(() => _error = 'Video này chưa có YouTube ID hợp lệ.');
      return;
    }
    _positionSubscription?.cancel();
    _playerValueSubscription?.cancel();
    _player?.close();
    debugPrint('YT select: id=${video.youtubeVideoId}, title=${video.title}');
    final player = YoutubePlayerController.fromVideoId(
      videoId: video.youtubeVideoId,
      params: const YoutubePlayerParams(
        // The learning controls below are the single source of playback UI.
        // Hiding YouTube's own chrome keeps the experience consistent on every
        // platform and with the Video Lab design.
        showControls: false,
        showFullscreenButton: false,
        enableCaption: false,
        enableKeyboard: false,
        showVideoAnnotations: false,
        strictRelatedVideos: true,
        pointerEvents: PointerEvents.none,
        playsInline: true,
        privacyEnhancedMode: false,
        // Android WebView has no Referer by default. Identify this installed
        // application so YouTube can authorize the embedded player.
        origin: 'https://com.example.kapor_flutter',
      ),
      autoPlay: true,
    );
    _positionSubscription = player.videoStateStream.listen((state) {
      if (mounted) {
        setState(() => _position = state.position.inMilliseconds / 1000);
      }
    });
    _playerValueSubscription = player.stream.listen((value) {
      debugPrint(
        'YT player: state=${value.playerState}, error=${value.error}, '
        'videoId=${value.metaData.videoId}, quality=${value.playbackQuality}',
      );
      if (mounted && value.hasError) {
        setState(() => _error = 'YouTube player error: ${value.error}');
      }
      final duration = value.metaData.duration.inMilliseconds / 1000;
      if (mounted && duration > 0 && duration != _durationSeconds) {
        setState(() => _durationSeconds = duration);
      }
      if (mounted && value.playbackRate != _playbackRate) {
        setState(() => _playbackRate = value.playbackRate);
      }
      final isPlaying = value.playerState == PlayerState.playing;
      if (mounted && isPlaying != _isPlaying) {
        setState(() => _isPlaying = isPlaying);
      }
    });
    setState(() {
      _video = video;
      _player = player;
      _position = 0;
      _durationSeconds = video.durationSeconds.toDouble();
      _playbackRate = 1;
      _isPlaying = true;
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final isWatchingVideo = _video != null;
    return PopScope(
      canPop: !isWatchingVideo,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && isWatchingVideo) {
          _goHome();
        }
      },
      child: Scaffold(
        backgroundColor: AppTheme.background,
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: isWatchingVideo ? _goHome : context.pop,
          ),
          title: Text(_video == null ? 'Video Lab' : _video!.title),
        ),
        body: SafeArea(child: _video == null ? _library() : _playerView()),
      ),
    );
  }

  void _goHome() {
    _player?.pauseVideo();
    context.go('/dashboard');
  }

  Widget _library() => FutureBuilder<List<LearningVideo>>(
    future: _videosFuture,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done) {
        return const Center(child: CircularProgressIndicator());
      }
      if (snapshot.hasError) {
        return _centerMessage(
          snapshot.error.toString().replaceFirst('Exception: ', ''),
          retry: () => setState(() => _videosFuture = _service.getVideos()),
        );
      }
      final videos = snapshot.data ?? const [];
      if (videos.isEmpty) {
        return _centerMessage(
          'Chưa có video. Hãy thêm nội dung từ Admin Panel.',
        );
      }
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'VIDEO IT HÀN QUỐC',
            style: GoogleFonts.jetBrainsMono(
              fontSize: 10,
              color: AppTheme.textSecondary,
              letterSpacing: 1,
            ),
          ),
          const SizedBox(height: 10),
          ...videos.map(
            (video) => Card(
              child: ListTile(
                onTap: () => _select(video),
                leading: const CircleAvatar(
                  backgroundColor: AppTheme.primary,
                  child: Icon(Icons.play_arrow, color: Colors.black),
                ),
                title: Text(
                  video.title,
                  style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                ),
                subtitle: Text(
                  video.titleVi.isEmpty ? video.domain : video.titleVi,
                  style: GoogleFonts.inter(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
                trailing: const Icon(Icons.chevron_right),
              ),
            ),
          ),
        ],
      );
    },
  );

  Widget _playerView() {
    final video = _video!;
    final korean = _active(video.koreanSubtitles);
    final vietnamese = _active(video.vietnameseSubtitles);
    final subtitleIndex = _subtitleIndexAt(video.koreanSubtitles);
    final canGoPrevious = subtitleIndex > 0;
    final canGoNext =
        subtitleIndex >= 0 && subtitleIndex < video.koreanSubtitles.length - 1;
    final duration = _durationSeconds > 0
        ? _durationSeconds
        : video.durationSeconds.toDouble();
    _maybeOpenQuiz(video);
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        AspectRatio(
          aspectRatio: 16 / 9,
          child: YoutubePlayer(
            controller: _player!,
            // Android renders the YouTube iframe in a platform view, which
            // sits above an ordinary Flutter Stack. `controlsBuilder` is
            // rendered by the player's overlay portal, so subtitles remain
            // visible above the video on Android as well as web/iOS.
            autoFullScreen: false,
            enableFullScreenOnVerticalDrag: false,
            controlsBuilder: (context, isFullscreen) {
              if (isFullscreen || (korean == null && vietnamese == null)) {
                return const SizedBox.shrink();
              }
              return Padding(
                padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                child: _subtitleOverlay(korean, vietnamese),
              );
            },
          ),
        ),
        Container(
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(color: Colors.white.withValues(alpha: .06)),
            ),
          ),
          padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
          child: _videoControls(
            duration: duration,
            subtitleIndex: subtitleIndex,
            canGoPrevious: canGoPrevious,
            canGoNext: canGoNext,
            subtitles: video.koreanSubtitles,
            quizzes: video.quizzes,
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'TỪ VỰNG ĐOẠN NÀY',
                style: GoogleFonts.jetBrainsMono(
                  fontSize: 10,
                  color: AppTheme.textSecondary,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: (korean?.tokens ?? const [])
                    .where((token) => token.clickable)
                    .map(
                      (token) => ActionChip(
                        backgroundColor: _videoTeal.withValues(alpha: .12),
                        side: BorderSide(
                          color: _videoTeal.withValues(alpha: .4),
                        ),
                        label: Text(
                          token.surface,
                          style: GoogleFonts.outfit(
                            color: _videoTeal,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        onPressed: () => _showWord(token),
                      ),
                    )
                    .toList(),
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Text(
                    _error!,
                    style: const TextStyle(color: Colors.redAccent),
                  ),
                ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () {
                  _player?.pauseVideo();
                  setState(() {
                    _video = null;
                    _player = null;
                  });
                },
                icon: const Icon(Icons.video_library),
                label: const Text('Đổi video'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  VideoSubtitle? _active(List<VideoSubtitle> subtitles) {
    for (final item in subtitles) {
      if (_position >= item.start && _position < item.end) return item;
    }
    return null;
  }

  int _subtitleIndexAt(List<VideoSubtitle> subtitles) {
    if (subtitles.isEmpty) return -1;
    for (var index = 0; index < subtitles.length; index++) {
      final subtitle = subtitles[index];
      if (_position >= subtitle.start && _position < subtitle.end) return index;
    }
    if (_position < subtitles.first.start) return 0;
    return subtitles.length - 1;
  }

  Future<void> _seekTo(double seconds) async {
    final duration = _durationSeconds;
    final target = duration > 0
        ? seconds.clamp(0, duration).toDouble()
        : seconds;
    setState(() => _position = target);
    await _player?.seekTo(seconds: target, allowSeekAhead: true);
  }

  void _seekFromTrackPosition(double dx, double width, double duration) {
    if (width <= 0 || duration <= 0) return;
    final fraction = (dx / width).clamp(0.0, 1.0).toDouble();
    _seekTo(fraction * duration);
  }

  Future<void> _setPlaybackRate(double rate) async {
    setState(() => _playbackRate = rate);
    await _player?.setPlaybackRate(rate);
  }

  Future<void> _togglePlayback() async {
    if (_isPlaying) {
      await _player?.pauseVideo();
    } else {
      await _player?.playVideo();
    }
  }

  Widget _videoControls({
    required double duration,
    required int subtitleIndex,
    required bool canGoPrevious,
    required bool canGoNext,
    required List<VideoSubtitle> subtitles,
    required List<VideoQuiz> quizzes,
  }) => Column(
    children: [
      _progressBar(duration: duration, quizzes: quizzes),
      const SizedBox(height: 10),
      Row(
        children: [
          Tooltip(
            message: _isPlaying ? 'Tạm dừng' : 'Phát video',
            child: Material(
              color: _videoTeal,
              shape: const CircleBorder(),
              child: InkWell(
                onTap: _togglePlayback,
                customBorder: const CircleBorder(),
                child: SizedBox(
                  width: 32,
                  height: 32,
                  child: Icon(
                    _isPlaying ? Icons.pause : Icons.play_arrow,
                    size: 15,
                    color: Colors.black,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              '${_formatVideoTime(_position)} / ${_formatVideoTime(duration)}',
              style: GoogleFonts.jetBrainsMono(
                fontSize: 10,
                color: const Color(0xFF64748B),
              ),
            ),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [.75, 1.0, 1.25]
                .map(
                  (rate) => Padding(
                    padding: const EdgeInsets.only(left: 4),
                    child: _speedButton(rate),
                  ),
                )
                .toList(),
          ),
        ],
      ),
      const SizedBox(height: 8),
      Row(
        children: [
          Expanded(
            child: _subtitleNavigationButton(
              label: '← Trước',
              enabled: canGoPrevious,
              onTap: () => _seekTo(subtitles[subtitleIndex - 1].start),
            ),
          ),
          const SizedBox(width: 6),
          Expanded(
            child: _subtitleNavigationButton(
              label: 'Tiếp →',
              enabled: canGoNext,
              onTap: () => _seekTo(subtitles[subtitleIndex + 1].start),
            ),
          ),
        ],
      ),
    ],
  );

  Widget _progressBar({
    required double duration,
    required List<VideoQuiz> quizzes,
  }) {
    final progress = duration > 0
        ? (_position / duration).clamp(0.0, 1.0).toDouble()
        : 0.0;
    return LayoutBuilder(
      builder: (context, constraints) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: duration > 0
            ? (details) => _seekFromTrackPosition(
                details.localPosition.dx,
                constraints.maxWidth,
                duration,
              )
            : null,
        onHorizontalDragUpdate: duration > 0
            ? (details) => _seekFromTrackPosition(
                details.localPosition.dx,
                constraints.maxWidth,
                duration,
              )
            : null,
        child: SizedBox(
          height: 12,
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              Center(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(99),
                  child: SizedBox(
                    height: 6,
                    width: double.infinity,
                    child: Stack(
                      children: [
                        const ColoredBox(color: Color(0xFF122131)),
                        FractionallySizedBox(
                          widthFactor: progress,
                          child: const ColoredBox(color: _videoTeal),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              ...quizzes
                  .where((quiz) => duration > 0 && quiz.timestamp <= duration)
                  .map((quiz) {
                    final markerPosition = (quiz.timestamp / duration)
                        .clamp(0.0, 1.0)
                        .toDouble();
                    final passed = quiz.timestamp <= _position;
                    return Positioned(
                      left: constraints.maxWidth * markerPosition - 6,
                      top: 0,
                      child: Tooltip(
                        message:
                            'Câu hỏi tại ${_formatVideoTime(quiz.timestamp)}',
                        child: Container(
                          width: 12,
                          height: 12,
                          decoration: BoxDecoration(
                            color: passed
                                ? const Color(0xFF34D399)
                                : _videoAmber,
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.black, width: 2),
                          ),
                        ),
                      ),
                    );
                  }),
            ],
          ),
        ),
      ),
    );
  }

  Widget _speedButton(double rate) {
    final selected = _playbackRate == rate;
    final label = rate == 1 ? '1×' : '$rate×';
    return Material(
      color: selected ? _videoTeal : const Color(0xFF122131),
      borderRadius: BorderRadius.circular(5),
      child: InkWell(
        onTap: () => _setPlaybackRate(rate),
        borderRadius: BorderRadius.circular(5),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 5),
          child: Text(
            label,
            style: GoogleFonts.jetBrainsMono(
              color: selected ? Colors.black : const Color(0xFF64748B),
              fontSize: 9,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }

  Widget _subtitleNavigationButton({
    required String label,
    required bool enabled,
    required VoidCallback onTap,
  }) => Material(
    color: const Color(0xFF0E1B2B),
    borderRadius: BorderRadius.circular(8),
    child: InkWell(
      onTap: enabled ? onTap : null,
      borderRadius: BorderRadius.circular(8),
      child: SizedBox(
        height: 36,
        child: Center(
          child: Text(
            label,
            style: GoogleFonts.jetBrainsMono(
              color: enabled
                  ? const Color(0xFF8A9AAD)
                  : const Color(0xFF4B5A6B),
              fontSize: 11,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    ),
  );

  String _formatVideoTime(double seconds) {
    final totalSeconds =
        (seconds.isFinite ? seconds.floor().clamp(0, 359999) : 0).toInt();
    final minutes = totalSeconds ~/ 60;
    final remainingSeconds = totalSeconds % 60;
    return '$minutes:${remainingSeconds.toString().padLeft(2, '0')}';
  }

  Widget _subtitleOverlay(VideoSubtitle? korean, VideoSubtitle? vietnamese) =>
      Align(
        alignment: Alignment.bottomCenter,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.black.withValues(alpha: .75),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Wrap(
                alignment: WrapAlignment.center,
                spacing: 4,
                runSpacing: 2,
                children: (korean?.tokens ?? const [])
                    .map(
                      (token) => TextButton(
                        onPressed: token.clickable
                            ? () => _showWord(token)
                            : null,
                        style: TextButton.styleFrom(
                          foregroundColor: Colors.white,
                          minimumSize: Size.zero,
                          padding: EdgeInsets.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          textStyle: GoogleFonts.outfit(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            decoration: TextDecoration.underline,
                            decorationStyle: TextDecorationStyle.dotted,
                            decorationColor: Colors.white54,
                          ),
                        ),
                        child: Text(token.surface),
                      ),
                    )
                    .toList(),
              ),
              if ((korean?.tokens ?? const []).isEmpty)
                Text(
                  korean?.text ?? '…',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.outfit(
                    fontSize: 14,
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              if ((vietnamese?.text ?? '').isNotEmpty) ...[
                const SizedBox(height: 3),
                Text(
                  vietnamese!.text,
                  textAlign: TextAlign.center,
                  style: GoogleFonts.inter(fontSize: 10, color: Colors.white60),
                ),
              ],
            ],
          ),
        ),
      );
  void _showWord(VideoToken token) {
    _player?.pauseVideo();
    final video = _video;
    if (video == null) return;
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => _WordDetailSheet(
        token: token,
        deckTitle: video.title,
        onSave: () => _service.saveVideoToken(video.id, token.surface),
      ),
    );
  }

  void _maybeOpenQuiz(LearningVideo video) {
    for (final quiz in video.quizzes) {
      if (_position >= quiz.timestamp && !_completedQuizzes.contains(quiz.id)) {
        _completedQuizzes.add(quiz.id);
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) _quiz(video, quiz);
        });
        break;
      }
    }
  }

  Future<void> _quiz(LearningVideo video, VideoQuiz quiz) async {
    await _player?.pauseVideo();
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(quiz.questionVi.isEmpty ? quiz.question : quiz.questionVi),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(
            quiz.options.length,
            (index) => ListTile(
              title: Text(quiz.options[index]),
              onTap: () async {
                Navigator.pop(context);
                try {
                  final correct = await _service.answerQuiz(
                    video.id,
                    quiz.id,
                    index,
                  );
                  if (mounted) {
                    ScaffoldMessenger.of(this.context).showSnackBar(
                      SnackBar(
                        content: Text(
                          correct
                              ? 'Chính xác!'
                              : 'Chưa đúng, hãy xem lại đoạn video.',
                        ),
                      ),
                    );
                  }
                } catch (_) {
                  if (mounted) {
                    setState(() => _error = 'Không thể gửi đáp án quiz.');
                  }
                }
              },
            ),
          ),
        ),
      ),
    );
  }

  Widget _centerMessage(String text, {VoidCallback? retry}) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            text,
            textAlign: TextAlign.center,
            style: GoogleFonts.inter(color: AppTheme.textSecondary),
          ),
          if (retry != null)
            TextButton(onPressed: retry, child: const Text('Thử lại')),
        ],
      ),
    ),
  );
}

class _WordDetailSheet extends StatefulWidget {
  const _WordDetailSheet({
    required this.token,
    required this.deckTitle,
    required this.onSave,
  });

  final VideoToken token;
  final String deckTitle;
  final Future<bool> Function() onSave;

  @override
  State<_WordDetailSheet> createState() => _WordDetailSheetState();
}

class _WordDetailSheetState extends State<_WordDetailSheet> {
  bool _saving = false;
  bool _saved = false;

  String _value(String value, String fallback) =>
      value.trim().isEmpty ? fallback : value.trim();

  Future<void> _saveToMembyte() async {
    if (_saving || _saved) return;
    setState(() => _saving = true);
    try {
      final alreadySaved = await widget.onSave();
      if (!mounted) {
        return;
      }
      setState(() {
        _saving = false;
        _saved = true;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            alreadySaved
                ? 'Từ này đã có trong bộ thẻ MemByte.'
                : 'Đã thêm vào bộ thẻ MemByte.',
          ),
        ),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final token = widget.token;
    final pronunciation = _value(token.pronunciation, 'Chưa có phiên âm');
    final vi = _value(token.meaningVi, 'Chưa có nghĩa tiếng Việt');
    final en = _value(token.meaningEn, 'English meaning unavailable');
    final definition = _value(
      token.definitionEn,
      'English definition unavailable',
    );
    final example = _value(token.exampleKo, 'Chưa có câu ví dụ.');

    return SafeArea(
      top: false,
      child: Container(
        width: double.infinity,
        constraints: const BoxConstraints(maxHeight: 560),
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
        decoration: const BoxDecoration(
          color: Color(0xFF06111E),
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 42,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white24,
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          token.surface,
                          style: GoogleFonts.outfit(
                            color: AppTheme.primary,
                            fontSize: 32,
                            height: 1,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 7),
                        Text(
                          '$pronunciation  ·  Từ gốc: ${token.stem}',
                          style: GoogleFonts.inter(
                            color: AppTheme.textSecondary,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Đóng',
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close),
                    style: IconButton.styleFrom(
                      backgroundColor: const Color(0xFF102236),
                      foregroundColor: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 22),
              _detailRow('NGHĨA', '$vi ($en)', AppTheme.textPrimary),
              const SizedBox(height: 16),
              _detailRow(
                'ENGLISH DEFINITION',
                definition,
                const Color(0xFFB7C8DA),
              ),
              const SizedBox(height: 16),
              _detailRow(
                'VÍ DỤ TIẾNG HÀN',
                example,
                AppTheme.textPrimary,
                italic: true,
              ),
              const SizedBox(height: 22),
              Text(
                'BỘ THẺ MEMBYTE',
                style: GoogleFonts.jetBrainsMono(
                  color: AppTheme.primary,
                  fontSize: 10,
                  fontWeight: FontWeight.w700,
                  letterSpacing: .8,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                widget.deckTitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.inter(
                  color: AppTheme.textSecondary,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _saving || _saved ? null : _saveToMembyte,
                  icon: _saving
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Icon(_saved ? Icons.check : Icons.add),
                  label: Text(
                    _saving
                        ? 'Đang thêm...'
                        : _saved
                        ? 'Đã thêm vào MemByte'
                        : 'Thêm vào MemByte',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _detailRow(
    String label,
    String value,
    Color color, {
    bool italic = false,
  }) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        label,
        style: GoogleFonts.jetBrainsMono(
          color: AppTheme.primary,
          fontSize: 10,
          fontWeight: FontWeight.w700,
          letterSpacing: .8,
        ),
      ),
      const SizedBox(height: 6),
      Text(
        value,
        style: GoogleFonts.inter(
          color: color,
          fontSize: 16,
          height: 1.4,
          fontStyle: italic ? FontStyle.italic : FontStyle.normal,
        ),
      ),
    ],
  );
}

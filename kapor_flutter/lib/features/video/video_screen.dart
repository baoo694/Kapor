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
  final _service = VideoService();
  late Future<List<LearningVideo>> _videosFuture;
  LearningVideo? _video;
  YoutubePlayerController? _player;
  StreamSubscription<YoutubeVideoState>? _positionSubscription;
  StreamSubscription<YoutubePlayerValue>? _playerValueSubscription;
  double _position = 0;
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
        showControls: true,
        showFullscreenButton: true,
        playsInline: true,
        privacyEnhancedMode: false,
        // Android WebView has no Referer by default. Identify this installed
        // application so YouTube can authorize the embedded player.
        origin: 'https://com.example.kapor_flutter',
      ),
      autoPlay: true,
    );
    _positionSubscription = player.videoStateStream.listen((state) {
      if (mounted)
        setState(() => _position = state.position.inMilliseconds / 1000);
    });
    _playerValueSubscription = player.stream.listen((value) {
      debugPrint(
        'YT player: state=${value.playerState}, error=${value.error}, '
        'videoId=${value.metaData.videoId}, quality=${value.playbackQuality}',
      );
      if (mounted && value.hasError) {
        setState(() => _error = 'YouTube player error: ${value.error}');
      }
    });
    setState(() {
      _video = video;
      _player = player;
      _position = 0;
      _error = null;
    });
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
        title: Text(_video == null ? 'Video Lab' : _video!.title),
      ),
      body: SafeArea(child: _video == null ? _library() : _playerView()),
    );
  }

  Widget _library() => FutureBuilder<List<LearningVideo>>(
    future: _videosFuture,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done)
        return const Center(child: CircularProgressIndicator());
      if (snapshot.hasError)
        return _centerMessage(
          snapshot.error.toString().replaceFirst('Exception: ', ''),
          retry: () => setState(() => _videosFuture = _service.getVideos()),
        );
      final videos = snapshot.data ?? const [];
      if (videos.isEmpty)
        return _centerMessage(
          'Chưa có video. Hãy thêm nội dung từ Admin Panel.',
        );
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
    _maybeOpenQuiz(video);
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        AspectRatio(
          aspectRatio: 16 / 9,
          child: YoutubePlayer(controller: _player!),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                video.titleVi,
                style: GoogleFonts.inter(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                ),
              ),
              const SizedBox(height: 16),
              _subtitleCard(korean, vietnamese),
              const SizedBox(height: 18),
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
                        backgroundColor: AppTheme.primary.withValues(
                          alpha: .12,
                        ),
                        side: BorderSide(
                          color: AppTheme.primary.withValues(alpha: .4),
                        ),
                        label: Text(
                          token.surface,
                          style: GoogleFonts.outfit(
                            color: AppTheme.primary,
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

  Widget _subtitleCard(VideoSubtitle? korean, VideoSubtitle? vietnamese) =>
      Container(
        width: double.infinity,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.black87,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          children: [
            Text(
              korean?.text ?? '…',
              textAlign: TextAlign.center,
              style: GoogleFonts.outfit(
                fontSize: 19,
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 5),
            Text(
              vietnamese?.text ?? '',
              textAlign: TextAlign.center,
              style: GoogleFonts.inter(fontSize: 12, color: Colors.white70),
            ),
          ],
        ),
      );
  void _showWord(VideoToken token) {
    _player?.pauseVideo();
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => _WordDetailSheet(token: token),
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
                  if (mounted)
                    ScaffoldMessenger.of(this.context).showSnackBar(
                      SnackBar(
                        content: Text(
                          correct
                              ? 'Chính xác!'
                              : 'Chưa đúng, hãy xem lại đoạn video.',
                        ),
                      ),
                    );
                } catch (_) {
                  if (mounted)
                    setState(() => _error = 'Không thể gửi đáp án quiz.');
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

class _WordDetailSheet extends StatelessWidget {
  const _WordDetailSheet({required this.token});

  final VideoToken token;

  String _value(String value, String fallback) =>
      value.trim().isEmpty ? fallback : value.trim();

  @override
  Widget build(BuildContext context) {
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

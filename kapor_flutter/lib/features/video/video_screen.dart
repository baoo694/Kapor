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
    _player?.close();
    super.dispose();
  }

  void _select(LearningVideo video) {
    _positionSubscription?.cancel();
    _player?.close();
    final player = YoutubePlayerController.fromVideoId(
      videoId: video.youtubeVideoId,
      params: const YoutubePlayerParams(
        showControls: true,
        showFullscreenButton: true,
        playsInline: true,
      ),
    );
    _positionSubscription = player.videoStateStream.listen((state) {
      if (mounted)
        setState(() => _position = state.position.inMilliseconds / 1000);
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
                        label: Text(token.surface),
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
      backgroundColor: AppTheme.surface,
      builder: (context) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              token.surface,
              style: GoogleFonts.outfit(
                fontSize: 25,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Từ gốc: ${token.stem}',
              style: GoogleFonts.inter(color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            const Text(
              'Từ điển IT sẽ hiển thị nghĩa và nút thêm MemByte khi nội dung từ điển được admin cung cấp.',
            ),
            const SizedBox(height: 8),
          ],
        ),
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

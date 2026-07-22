import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../core/theme/app_theme.dart';
import 'data/pronunciation_service.dart';

class PronunciationListScreen extends StatefulWidget {
  const PronunciationListScreen({super.key});
  @override
  State<PronunciationListScreen> createState() =>
      _PronunciationListScreenState();
}

class _PronunciationListScreenState extends State<PronunciationListScreen> {
  final _service = PronunciationService();
  late Future<List<PronunciationExercise>> _future;

  @override
  void initState() {
    super.initState();
    _future = _service.exercises();
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
        title: const Text('Luyện phát âm'),
      ),
      body: FutureBuilder<List<PronunciationExercise>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done)
            return const Center(child: CircularProgressIndicator());
          if (snapshot.hasError) {
            return Center(
              child: TextButton(
                onPressed: () => setState(() => _future = _service.exercises()),
                child: Text(
                  snapshot.error.toString().replaceFirst('Exception: ', ''),
                ),
              ),
            );
          }
          final exercises = snapshot.data ?? const <PronunciationExercise>[];
          if (exercises.isEmpty) {
            return Center(
              child: Text(
                'Chưa có bài tập. Hãy thêm reference audio từ Admin Panel.',
                style: GoogleFonts.inter(color: AppTheme.textSecondary),
              ),
            );
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'BÀI TẬP SHADOWING',
                style: GoogleFonts.jetBrainsMono(
                  fontSize: 10,
                  color: AppTheme.textSecondary,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 12),
              ...exercises.map((exercise) {
                final sentence = exercise.sentences.isEmpty
                    ? ''
                    : exercise.sentences.first.text;
                return Card(
                  child: ListTile(
                    onTap: () =>
                        context.push('/pronunciation', extra: exercise),
                    title: Text(
                      exercise.title,
                      style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                    ),
                    subtitle: Text(
                      exercise.titleVi.isEmpty ? sentence : exercise.titleVi,
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    trailing: Text(
                      exercise.difficulty,
                      style: GoogleFonts.jetBrainsMono(
                        fontSize: 10,
                        color: AppTheme.secondary,
                      ),
                    ),
                  ),
                );
              }),
            ],
          );
        },
      ),
    );
  }
}

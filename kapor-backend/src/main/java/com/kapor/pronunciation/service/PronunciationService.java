package com.kapor.pronunciation.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.pronunciation.dto.PronunciationEvaluationDto;
import com.kapor.pronunciation.model.PronunciationAttempt;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.repository.PronunciationAttemptRepository;
import com.kapor.pronunciation.repository.PronunciationExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PronunciationService {
    private final PronunciationExerciseRepository exerciseRepository;
    private final PronunciationAttemptRepository attemptRepository;

    public List<PronunciationExercise> exercises() {
        List<PronunciationExercise> exercises = exerciseRepository.findAllByOrderByOrderAsc();
        if (!exercises.isEmpty()) return exercises;
        return List.of(
                exerciseRepository.save(defaultExercise("서버 배포 관련 문장", "Câu liên quan đến triển khai server", "devops", "intermediate",
                        "서버 배포가 완료되었습니다", "Việc triển khai server đã hoàn tất", 1)),
                exerciseRepository.save(defaultExercise("비동기 처리", "Câu về xử lý bất đồng bộ", "frontend", "beginner",
                        "비동기 처리를 구현했습니다", "Tôi đã triển khai xử lý bất đồng bộ", 2))
        );
    }

    public PronunciationExercise exercise(String id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pronunciation exercise", "id", id));
    }

    /** Stores a pending attempt. Scores are intentionally null until a Korean-capable
     * pronunciation assessment provider is configured; the API never invents scores. */
    public PronunciationEvaluationDto evaluate(String userId, String exerciseId, int sentenceIndex, byte[] audio) {
        PronunciationExercise exercise = exercise(exerciseId);
        if (sentenceIndex < 0 || sentenceIndex >= exercise.getSentences().size()) {
            throw new IllegalArgumentException("Invalid sentence index");
        }
        PronunciationAttempt attempt = attemptRepository.save(PronunciationAttempt.builder()
                .userId(userId).exerciseId(exerciseId).sentenceIndex(sentenceIndex).status("pending_provider")
                .userWaveform(waveform(audio)).attemptedAt(Instant.now()).build());
        return PronunciationEvaluationDto.builder().attemptId(attempt.getId()).status(attempt.getStatus())
                .message("Bản ghi đã được lưu. Cần cấu hình speech provider hỗ trợ tiếng Hàn để chấm điểm.")
                .scores(null).transcription(List.of())
                .referenceWaveform(exercise.getSentences().get(sentenceIndex).getWaveformData())
                .userWaveform(attempt.getUserWaveform()).build();
    }

    public List<PronunciationAttempt> history(String userId) { return attemptRepository.findByUserIdOrderByAttemptedAtDesc(userId); }

    private List<Double> waveform(byte[] audio) {
        if (audio == null || audio.length == 0) return List.of();
        int buckets = Math.min(64, Math.max(8, audio.length / 256));
        java.util.ArrayList<Double> values = new java.util.ArrayList<>();
        for (int bucket = 0; bucket < buckets; bucket++) {
            int start = bucket * audio.length / buckets;
            int end = Math.max(start + 1, (bucket + 1) * audio.length / buckets);
            long sum = 0;
            for (int index = start; index < end; index++) sum += Math.abs(audio[index]);
            values.add(Math.min(1d, sum / (double) ((end - start) * 128)));
        }
        return values;
    }

    private PronunciationExercise defaultExercise(String title, String titleVi, String domain, String difficulty,
                                                  String text, String translation, int order) {
        return PronunciationExercise.builder().title(title).titleVi(titleVi).domain(domain).difficulty(difficulty)
                .order(order).createdAt(Instant.now()).sentences(List.of(PronunciationExercise.Sentence.builder()
                        .text(text).translationVi(translation).waveformData(List.of(.12, .32, .64, .42, .25, .55, .3, .15)).build()))
                .build();
    }
}

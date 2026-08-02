package com.kapor.pronunciation.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.pronunciation.dto.PronunciationEvaluationDto;
import com.kapor.pronunciation.dto.PronunciationAttemptDto;
import com.kapor.pronunciation.exception.PronunciationAssessmentException;
import com.kapor.pronunciation.model.PronunciationAttempt;
import com.kapor.pronunciation.model.PronunciationExercise;
import com.kapor.pronunciation.repository.PronunciationAttemptRepository;
import com.kapor.pronunciation.repository.PronunciationExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PronunciationService {
    /** Azure PA REST evaluates short scripted recordings; leave one second of headroom. */
    private static final int MAX_RECORDING_SECONDS = 29;
    private static final Duration AUDIO_RETENTION = Duration.ofDays(7);

    private final PronunciationExerciseRepository exerciseRepository;
    private final PronunciationAttemptRepository attemptRepository;
    private final PronunciationAudioStorage audioStorage;
    private final PronunciationAssessmentProvider assessmentProvider;
    private final KoreanReadingMatchScorer readingMatchScorer;

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

    public PronunciationEvaluationDto evaluate(String userId, String exerciseId, int sentenceIndex, byte[] pcm) {
        PronunciationExercise exercise = exercise(exerciseId);
        if (sentenceIndex < 0 || sentenceIndex >= exercise.getSentences().size()) {
            throw new IllegalArgumentException("Invalid sentence index");
        }
        long durationMs = PcmWavConverter.durationMs(pcm);
        if (durationMs > MAX_RECORDING_SECONDS * 1000L) {
            throw new IllegalArgumentException("Bản ghi không được dài quá " + MAX_RECORDING_SECONDS + " giây.");
        }
        byte[] wav = PcmWavConverter.toWav(pcm);
        Instant now = Instant.now();
        String audioObjectKey;
        try {
            audioObjectKey = audioStorage.storeAttempt(userId, wav);
        } catch (RuntimeException exception) {
            log.error("Could not store pronunciation recording for user {}", userId, exception);
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể lưu bản ghi phát âm. Hãy kiểm tra MinIO rồi thử lại.", exception);
        }
        PronunciationAttempt attempt;
        try {
            attempt = attemptRepository.save(PronunciationAttempt.builder()
                .userId(userId).exerciseId(exerciseId).sentenceIndex(sentenceIndex).status("processing")
                .provider(assessmentProvider.name()).assessmentVersion("azure-pa-whisperx-v2")
                .audioObjectKey(audioObjectKey).audioContentType("audio/wav")
                .audioDurationMs(durationMs).userWaveform(waveform(pcm)).attemptedAt(now)
                .expiresAt(now.plus(AUDIO_RETENTION)).build());
        } catch (RuntimeException exception) {
            log.error("Could not create pronunciation attempt for user {}", userId, exception);
            throw new PronunciationAssessmentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể lưu phiên đánh giá phát âm. Hãy kiểm tra MongoDB rồi thử lại.", exception);
        }
        try {
            String referenceText = exercise.getSentences().get(sentenceIndex).getText();
            PronunciationAssessmentProvider.Assessment assessment = assessmentProvider.assess(
                    userId, referenceText, wav);
            String transcriptionText = transcriptionText(assessment);
            Integer azureCompleteness = assessment.azureCompleteness() != null
                    ? assessment.azureCompleteness()
                    : assessment.scores() == null ? null : assessment.scores().getCompleteness();
            boolean differentSentence = readingMatchScorer.isDifferentSentence(
                    referenceText, transcriptionText, azureCompleteness);
            attempt.setStatus(differentSentence ? "wrong_sentence" : "completed");
            attempt.setScores(assessment.scores());
            attempt.setTranscriptionText(transcriptionText);
            attempt.setTranscript(assessment.transcript());
            attempt.setAssessmentWords(assessment.wordFeedback());
            // Compatibility for clients released before the evidence split.
            attempt.setTranscription(assessment.wordFeedback());
            attempt.setAnalysis(assessment.analysis());
            attempt = attemptRepository.save(attempt);
            String message = differentSentence
                    ? wrongSentenceMessage(transcriptionText)
                    : "Azure đã đánh giá phát âm; WhisperX đã tạo transcript và timeline.";
            return evaluation(attempt, exercise, message);
        } catch (WhisperPreflightRejectedException exception) {
            PronunciationAttempt.Transcript transcript = exception.transcript();
            String transcriptionText = transcript == null || transcript.getText() == null ? "" : transcript.getText().trim();
            attempt.setStatus("wrong_sentence");
            attempt.setProvider("whisperx_preflight");
            attempt.setAssessmentVersion("whisperx-preflight-v1");
            attempt.setScores(null);
            attempt.setTranscriptionText(transcriptionText);
            attempt.setTranscript(transcript);
            attempt.setAssessmentWords(List.of());
            attempt.setTranscription(List.of());
            attempt.setAnalysis(null);
            attempt = attemptRepository.save(attempt);
            return evaluation(attempt, exercise, wrongSentenceMessage(transcriptionText));
        } catch (RuntimeException exception) {
            attempt.setStatus("provider_error");
            attemptRepository.save(attempt);
            throw exception;
        }
    }

    public List<PronunciationAttemptDto> history(String userId, String exerciseId) {
        List<PronunciationAttempt> attempts = exerciseId == null || exerciseId.isBlank()
                ? attemptRepository.findByUserIdOrderByAttemptedAtDesc(userId)
                : attemptRepository.findByUserIdAndExerciseIdOrderByAttemptedAtDesc(userId, exerciseId);
        return attempts.stream().map(this::historyEntry).toList();
    }

    public PronunciationAudioStorage.AudioData attemptAudio(String userId, String attemptId) {
        PronunciationAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Pronunciation attempt", "id", attemptId));
        if (!userId.equals(attempt.getUserId())) throw new AccessDeniedException("Bạn không có quyền nghe bản ghi này.");
        if (attempt.getAudioObjectKey() == null || attempt.getAudioObjectKey().isBlank()) {
            throw new ResourceNotFoundException("Pronunciation recording", "attemptId", attemptId);
        }
        return audioStorage.read(attempt.getAudioObjectKey());
    }

    public void deleteExpiredAudio() {
        attemptRepository.findByExpiresAtBeforeAndAudioObjectKeyIsNotNull(Instant.now()).forEach(attempt -> {
            audioStorage.deleteQuietly(attempt.getAudioObjectKey());
            attempt.setAudioObjectKey(null);
            attempt.setAudioContentType(null);
            attemptRepository.save(attempt);
        });
    }

    private PronunciationEvaluationDto evaluation(PronunciationAttempt attempt, PronunciationExercise exercise, String message) {
        return PronunciationEvaluationDto.builder().attemptId(attempt.getId()).status(attempt.getStatus()).message(message)
                .assessmentVersion(attempt.getAssessmentVersion()).assessmentProvider(assessmentProvider(attempt))
                .transcriptProvider(transcriptProvider(attempt)).scores(attempt.getScores())
                .transcriptionText(attempt.getTranscriptionText()).transcript(attempt.getTranscript()).analysis(attempt.getAnalysis())
                .assessmentWords(attempt.getAssessmentWords())
                .transcription(attempt.getTranscription())
                .referenceWaveform(exercise.getSentences().get(attempt.getSentenceIndex()).getWaveformData())
                .userWaveform(attempt.getUserWaveform())
                .attemptAudioUrl("/pronunciation/attempts/" + attempt.getId() + "/audio").build();
    }

    private PronunciationAttemptDto historyEntry(PronunciationAttempt attempt) {
        return PronunciationAttemptDto.builder().id(attempt.getId()).exerciseId(attempt.getExerciseId())
                .sentenceIndex(attempt.getSentenceIndex()).status(attempt.getStatus()).assessmentVersion(attempt.getAssessmentVersion())
                .assessmentProvider(assessmentProvider(attempt)).transcriptProvider(transcriptProvider(attempt)).scores(attempt.getScores())
                .transcriptionText(attempt.getTranscriptionText()).transcript(attempt.getTranscript()).analysis(attempt.getAnalysis())
                .assessmentWords(attempt.getAssessmentWords()).transcription(attempt.getTranscription())
                .userWaveform(attempt.getUserWaveform()).attemptedAt(attempt.getAttemptedAt())
                .audioDurationMs(attempt.getAudioDurationMs())
                .attemptAudioUrl(attempt.getAudioObjectKey() == null ? "" : "/pronunciation/attempts/" + attempt.getId() + "/audio")
                .build();
    }

    private String assessmentProvider(PronunciationAttempt attempt) {
        return attempt.getProvider() == null ? "legacy" : attempt.getProvider().contains("azure") ? "azure-pa" : attempt.getProvider();
    }

    private String transcriptProvider(PronunciationAttempt attempt) {
        if (attempt.getTranscript() != null && attempt.getTranscript().getProvider() != null) {
            return attempt.getTranscript().getProvider();
        }
        return attempt.getProvider() != null && attempt.getProvider().contains("whisper") ? "whisperx" : "legacy";
    }

    private String transcriptionText(PronunciationAssessmentProvider.Assessment assessment) {
        if (assessment.transcription() != null && !assessment.transcription().isBlank()) {
            return assessment.transcription().trim();
        }
        if (assessment.transcript() != null && assessment.transcript().getText() != null) {
            return assessment.transcript().getText().trim();
        }
        return "";
    }

    private String wrongSentenceMessage(String transcriptionText) {
        return transcriptionText == null || transcriptionText.isBlank()
                ? "WhisperX chưa nhận dạng được câu mẫu trong bản ghi. Hãy nghe lại câu mẫu và đọc lại từ đầu."
                : "WhisperX nhận được nội dung khác câu mẫu. Hãy nghe lại câu mẫu và đọc lại từ đầu.";
    }

    private List<Double> waveform(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return List.of();
        int buckets = Math.min(64, Math.max(8, pcm.length / 512));
        java.util.ArrayList<Double> values = new java.util.ArrayList<>();
        for (int bucket = 0; bucket < buckets; bucket++) {
            int start = bucket * pcm.length / buckets;
            int end = Math.max(start + 1, (bucket + 1) * pcm.length / buckets);
            long sum = 0;
            for (int index = start; index < end; index += 2) {
                int sample = (pcm[index] & 0xff) | (pcm[Math.min(index + 1, pcm.length - 1)] << 8);
                sum += Math.abs((short) sample);
            }
            values.add(Math.min(1d, sum / (double) (Math.max(1, (end - start) / 2) * Short.MAX_VALUE)));
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

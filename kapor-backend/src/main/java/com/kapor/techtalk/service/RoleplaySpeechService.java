package com.kapor.techtalk.service;

import com.kapor.common.exception.ResourceNotFoundException;
import com.kapor.pronunciation.model.PronunciationAttempt;
import com.kapor.pronunciation.service.WhisperXTranscriber;
import com.kapor.techtalk.dto.RoleplayTranscriptionDto;
import com.kapor.techtalk.model.RoleplayAudioAsset;
import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.repository.RoleplayAudioAssetRepository;
import com.kapor.techtalk.repository.RoleplaySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleplaySpeechService {
    private static final int SAMPLE_RATE = 16_000;
    private static final int BYTES_PER_SECOND = SAMPLE_RATE * 2;

    private final RoleplaySessionRepository sessionRepository;
    private final RoleplayAudioAssetRepository assetRepository;
    private final WhisperXTranscriber transcriber;
    private final RoleplayAudioStorage storage;

    @Value("${techtalk.audio.retention-days:7}")
    private int retentionDays;

    public RoleplayTranscriptionDto transcribe(String userId, String sessionId, byte[] source, String contentType) {
        RoleplaySession session = sessionRepository.findById(sessionId)
                .filter(value -> userId.equals(value.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Roleplay session", "id", sessionId));
        if (!"active".equals(session.getStatus())) throw new IllegalArgumentException("Roleplay session is no longer active");
        if (source == null || source.length == 0) throw new IllegalArgumentException("Audio file is required");
        if (source.length > 2 * 1024 * 1024) throw new IllegalArgumentException("Bản ghi không được vượt quá 2 MB");

        byte[] wav = isWav(contentType, source) ? source : pcmToWav(source);
        long durationMs = durationMs(wav);
        if (durationMs < 250 || durationMs > 30_500) {
            throw new IllegalArgumentException("Bản ghi TechTalk phải dài từ 0.25 đến 30 giây.");
        }
        PronunciationAttempt.Transcript transcript = transcriber.transcribe(wav, "");
        if (transcript.getText() == null || transcript.getText().isBlank()) {
            throw new IllegalArgumentException("Không nhận diện được lời nói trong bản ghi.");
        }
        Double confidence = transcript.getWords().stream().map(PronunciationAttempt.TranscriptWord::getConfidence)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().stream().boxed().findFirst()
                .orElse(null);
        String objectKey = storage.store(userId, wav);
        Instant now = Instant.now();
        RoleplayAudioAsset asset = assetRepository.save(RoleplayAudioAsset.builder()
                .userId(userId).sessionId(sessionId).objectKey(objectKey).contentType("audio/wav")
                .durationMs(durationMs).transcript(transcript.getText()).confidence(confidence)
                .createdAt(now).expiresAt(now.plus(Duration.ofDays(Math.max(1, retentionDays)))).build());
        return RoleplayTranscriptionDto.builder().audioId(asset.getId()).transcript(asset.getTranscript())
                .confidence(confidence).durationMs(durationMs).build();
    }

    public byte[] audio(String userId, String sessionId, String audioId) {
        RoleplayAudioAsset asset = assetRepository.findByIdAndUserIdAndSessionId(audioId, userId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("TechTalk recording", "id", audioId));
        return storage.read(asset.getObjectKey());
    }

    public void validateAsset(String userId, String sessionId, String audioId) {
        if (audioId == null || audioId.isBlank()) {
            throw new IllegalArgumentException("Voice turns require a valid audio id");
        }
        assetRepository.findByIdAndUserIdAndSessionId(audioId, userId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Audio id does not belong to this roleplay session"));
    }

    @Scheduled(cron = "${techtalk.audio.cleanup-cron:0 25 3 * * *}")
    public void deleteExpiredAudio() {
        for (RoleplayAudioAsset asset : assetRepository.findByExpiresAtBefore(Instant.now())) {
            try {
                storage.delete(asset.getObjectKey());
                assetRepository.delete(asset);
            } catch (RuntimeException exception) {
                log.warn("Could not delete expired TechTalk recording {}", asset.getId(), exception);
            }
        }
    }

    private boolean isWav(String contentType, byte[] source) {
        return (contentType != null && contentType.toLowerCase().contains("wav"))
                || (source.length >= 12 && new String(source, 0, 4, StandardCharsets.US_ASCII).equals("RIFF"));
    }

    private byte[] pcmToWav(byte[] pcm) {
        if (pcm.length % 2 != 0) throw new IllegalArgumentException("Bản ghi PCM không hợp lệ.");
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(StandardCharsets.US_ASCII.encode("RIFF")).putInt(36 + pcm.length);
        header.put(StandardCharsets.US_ASCII.encode("WAVEfmt ")).putInt(16)
                .putShort((short) 1).putShort((short) 1)
                .putInt(SAMPLE_RATE).putInt(BYTES_PER_SECOND).putShort((short) 2).putShort((short) 16);
        header.put(StandardCharsets.US_ASCII.encode("data")).putInt(pcm.length);
        byte[] wav = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }

    private long durationMs(byte[] wav) {
        int dataLength = Math.max(0, wav.length - 44);
        return Math.round(dataLength * 1000d / BYTES_PER_SECOND);
    }
}

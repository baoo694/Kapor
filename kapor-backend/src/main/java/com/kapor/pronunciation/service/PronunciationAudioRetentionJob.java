package com.kapor.pronunciation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes private learner recordings after the retention period while retaining score history. */
@Component
@RequiredArgsConstructor
public class PronunciationAudioRetentionJob {
    private final PronunciationService pronunciationService;

    @Scheduled(cron = "${pronunciation.audio.cleanup-cron:0 20 3 * * *}")
    public void deleteExpiredRecordings() {
        pronunciationService.deleteExpiredAudio();
    }
}

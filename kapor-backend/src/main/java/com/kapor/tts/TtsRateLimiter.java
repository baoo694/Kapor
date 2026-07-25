package com.kapor.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Limits cache misses, which are the only requests that consume Gemini quota. */
@Service
public class TtsRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int maxRequests;
    private final ConcurrentMap<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public TtsRateLimiter(
            @Value("${gemini.tts.max-cache-misses-per-minute:12}") int maxRequests) {
        this.maxRequests = Math.max(1, maxRequests);
    }

    public boolean tryAcquire(String userId) {
        Deque<Instant> timestamps = requests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(WINDOW);
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) return false;
            timestamps.addLast(Instant.now());
            return true;
        }
    }
}

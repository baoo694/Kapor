package com.kapor.techtalk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class RoleplayRateLimiter {
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ConcurrentHashMap<String, Window> localWindows = new ConcurrentHashMap<>();

    @Value("${techtalk.quota.turns-per-minute:5}")
    private int turnsPerMinute;

    @Value("${techtalk.quota.turns-per-day:100}")
    private int turnsPerDay;

    @Value("${techtalk.quota.max-turns-per-session:30}")
    private int maxTurnsPerSession;

    public void checkTurn(String userId, String sessionId, int completedTurns, boolean testMode) {
        if (completedTurns >= maxTurnsPerSession) {
            throw new RoleplayRateLimitException("Phiên TechTalk đã đạt giới hạn số lượt.", 0);
        }
        int multiplier = testMode ? 2 : 1;
        consume("minute:" + userId, turnsPerMinute * multiplier, Duration.ofMinutes(1));
        consume("day:" + userId + ":" + LocalDate.now(ZoneOffset.UTC), turnsPerDay * multiplier, Duration.ofDays(2));
        consume("session:" + sessionId, maxTurnsPerSession, Duration.ofHours(12));
    }

    private void consume(String key, int limit, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                Long count = redis.opsForValue().increment("kapor:techtalk:" + key);
                if (count != null && count == 1) redis.expire("kapor:techtalk:" + key, ttl);
                if (count != null && count > limit) {
                    throw new RoleplayRateLimitException("Bạn đã gửi quá nhiều lượt TechTalk. Vui lòng thử lại sau.",
                            Math.max(1, ttl.toSeconds()));
                }
                return;
            } catch (RoleplayRateLimitException exception) {
                throw exception;
            } catch (RuntimeException ignored) {
                // Redis is optional in local/test environments. The local window
                // keeps the endpoint bounded until Redis becomes available.
            }
        }
        long now = Instant.now().toEpochMilli();
        Window window = localWindows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.expiresAt) return new Window(now + ttl.toMillis());
            return current;
        });
        if (window.count.incrementAndGet() > limit) {
            long retry = Math.max(1, (window.expiresAt - now) / 1000);
            throw new RoleplayRateLimitException("Bạn đã gửi quá nhiều lượt TechTalk. Vui lòng thử lại sau.", retry);
        }
        if (localWindows.size() > 10_000) {
            localWindows.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt);
        }
    }

    private static final class Window {
        private final long expiresAt;
        private final AtomicInteger count = new AtomicInteger();
        private Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}

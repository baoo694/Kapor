package com.kapor.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordResetOtpService {
    private static final String KEY_PREFIX = "kapor:auth:password-reset:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Atomically enforces the resend cooldown and fixed-window send quota before
     * replacing the current OTP. A new OTP also starts a fresh verification
     * attempt counter, but it cannot bypass an active verification lockout.
     */
    private static final DefaultRedisScript<Long> ISSUE_OTP_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return -2
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return -1
            end

            local sends = tonumber(redis.call('GET', KEYS[3]) or '0')
            if sends >= tonumber(ARGV[1]) then
                return -3
            end

            redis.call('SET', KEYS[2], '1', 'PX', ARGV[2])
            sends = redis.call('INCR', KEYS[3])
            if sends == 1 then
                redis.call('PEXPIRE', KEYS[3], ARGV[3])
            end

            redis.call('SET', KEYS[4], ARGV[4], 'PX', ARGV[5])
            redis.call('DEL', KEYS[5])
            return sends
            """, Long.class);

    /**
     * Verifies and consumes an OTP atomically. Incorrect attempts share the
     * OTP's remaining lifetime. Reaching the configured limit deletes the OTP
     * and creates a lock key so requesting another code cannot bypass it.
     */
    private static final DefaultRedisScript<Long> VERIFY_OTP_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 then
                return -2
            end

            local savedOtp = redis.call('GET', KEYS[1])
            if not savedOtp then
                return -1
            end

            if savedOtp == ARGV[1] then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 0
            end

            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then
                local remainingOtpTtl = redis.call('PTTL', KEYS[1])
                if remainingOtpTtl > 0 then
                    redis.call('PEXPIRE', KEYS[2], remainingOtpTtl)
                else
                    redis.call('PEXPIRE', KEYS[2], ARGV[3])
                end
            end

            if attempts >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[3], '1', 'PX', ARGV[4])
                redis.call('DEL', KEYS[1], KEYS[2])
                return -2
            end

            return attempts
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration otpTtl;
    private final Duration resendCooldown;
    private final int maxSendsPerWindow;
    private final Duration sendWindow;
    private final int maxVerificationAttempts;
    private final Duration verificationLockout;

    public PasswordResetOtpService(
            StringRedisTemplate redisTemplate,
            @Value("${password-reset.otp.ttl-minutes:15}") long otpTtlMinutes,
            @Value("${password-reset.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${password-reset.otp.max-sends-per-window:5}") int maxSendsPerWindow,
            @Value("${password-reset.otp.send-window-minutes:60}") long sendWindowMinutes,
            @Value("${password-reset.otp.max-verification-attempts:5}") int maxVerificationAttempts,
            @Value("${password-reset.otp.lockout-minutes:15}") long verificationLockoutMinutes) {
        this.redisTemplate = redisTemplate;
        this.otpTtl = positiveDuration(otpTtlMinutes, TimeUnit.MINUTES, "OTP TTL");
        this.resendCooldown = positiveDuration(resendCooldownSeconds, TimeUnit.SECONDS, "resend cooldown");
        this.maxSendsPerWindow = positive(maxSendsPerWindow, "max sends per window");
        this.sendWindow = positiveDuration(sendWindowMinutes, TimeUnit.MINUTES, "send window");
        this.maxVerificationAttempts = positive(maxVerificationAttempts, "max verification attempts");
        this.verificationLockout = positiveDuration(verificationLockoutMinutes, TimeUnit.MINUTES, "verification lockout");
    }

    public String issueOtp(String email) {
        String suffix = keySuffix(email);
        String otp = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));

        Long result = redisTemplate.execute(
                ISSUE_OTP_SCRIPT,
                List.of(lockKey(suffix), cooldownKey(suffix), sendQuotaKey(suffix), otpKey(suffix), attemptsKey(suffix)),
                String.valueOf(maxSendsPerWindow),
                String.valueOf(resendCooldown.toMillis()),
                String.valueOf(sendWindow.toMillis()),
                otp,
                String.valueOf(otpTtl.toMillis()));

        if (result == null) {
            throw new IllegalStateException("Unable to create password reset OTP");
        }
        if (result == -1) {
            throw rateLimited("Please wait before requesting another OTP.", cooldownKey(suffix), resendCooldown);
        }
        if (result == -2) {
            throw rateLimited("Too many invalid OTP attempts. Please try again later.", lockKey(suffix), verificationLockout);
        }
        if (result == -3) {
            throw rateLimited("Too many password reset requests. Please try again later.", sendQuotaKey(suffix), sendWindow);
        }
        return otp;
    }

    public void verifyAndConsume(String email, String submittedOtp) {
        String suffix = keySuffix(email);
        Long result = redisTemplate.execute(
                VERIFY_OTP_SCRIPT,
                List.of(otpKey(suffix), attemptsKey(suffix), lockKey(suffix)),
                submittedOtp,
                String.valueOf(maxVerificationAttempts),
                String.valueOf(otpTtl.toMillis()),
                String.valueOf(verificationLockout.toMillis()));

        if (result == null) {
            throw new IllegalStateException("Unable to verify password reset OTP");
        }
        if (result == 0) {
            return;
        }
        if (result == -2) {
            throw rateLimited("Too many invalid OTP attempts. Please try again later.", lockKey(suffix), verificationLockout);
        }
        if (result == -1) {
            throw new InvalidPasswordResetOtpException("Invalid or expired OTP");
        }

        long remainingAttempts = Math.max(0, maxVerificationAttempts - result);
        throw new InvalidPasswordResetOtpException(
                "Invalid OTP. " + remainingAttempts + " attempt(s) remaining.");
    }

    public void invalidateOtp(String email) {
        String suffix = keySuffix(email);
        redisTemplate.delete(List.of(otpKey(suffix), attemptsKey(suffix)));
    }

    public long getOtpTtlMinutes() {
        return otpTtl.toMinutes();
    }

    private PasswordResetRateLimitException rateLimited(String message, String key, Duration fallback) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long retryAfterSeconds = ttl == null || ttl <= 0 ? fallback.toSeconds() : ttl;
        return new PasswordResetRateLimitException(message, Math.max(1, retryAfterSeconds));
    }

    private String keySuffix(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String otpKey(String suffix) {
        return KEY_PREFIX + "otp:" + suffix;
    }

    private String attemptsKey(String suffix) {
        return KEY_PREFIX + "attempts:" + suffix;
    }

    private String lockKey(String suffix) {
        return KEY_PREFIX + "lock:" + suffix;
    }

    private String cooldownKey(String suffix) {
        return KEY_PREFIX + "send-cooldown:" + suffix;
    }

    private String sendQuotaKey(String suffix) {
        return KEY_PREFIX + "send-quota:" + suffix;
    }

    private static Duration positiveDuration(long value, TimeUnit unit, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return Duration.ofMillis(unit.toMillis(value));
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }
}

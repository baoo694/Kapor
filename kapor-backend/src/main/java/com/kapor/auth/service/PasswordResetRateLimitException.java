package com.kapor.auth.service;

public class PasswordResetRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public PasswordResetRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

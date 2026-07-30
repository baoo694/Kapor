package com.kapor.techtalk.service;

public class RoleplayRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public RoleplayRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

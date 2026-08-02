package com.kapor.techtalk.service;

public class RoleplayAiException extends RuntimeException {
    private final boolean retryable;
    private final String code;

    public RoleplayAiException(String message, boolean retryable) {
        this("GEMINI_UNKNOWN", message, retryable, null);
    }

    public RoleplayAiException(String message, boolean retryable, Throwable cause) {
        this("GEMINI_UNKNOWN", message, retryable, cause);
    }

    public RoleplayAiException(String code, String message, boolean retryable) {
        this(code, message, retryable, null);
    }

    public RoleplayAiException(String code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code == null || code.isBlank() ? "GEMINI_UNKNOWN" : code;
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getCode() {
        return code;
    }
}

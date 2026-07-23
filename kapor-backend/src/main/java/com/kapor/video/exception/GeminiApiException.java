package com.kapor.video.exception;

import org.springframework.http.HttpStatus;

/** A sanitized Gemini failure that is safe to return to an admin client. */
public class GeminiApiException extends RuntimeException {
    private final HttpStatus status;

    public GeminiApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public GeminiApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

package com.kapor.pronunciation.exception;

import org.springframework.http.HttpStatus;

/** A sanitized failure from the local pronunciation assessment pipeline. */
public class PronunciationAssessmentException extends RuntimeException {
    private final HttpStatus status;

    public PronunciationAssessmentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public PronunciationAssessmentException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

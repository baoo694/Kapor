package com.kapor.summarizer;

public class SummarizerRateLimitException extends RuntimeException {
    public SummarizerRateLimitException(String message) { super(message); }
}

package com.kapor.auth.service;

public class InvalidPasswordResetOtpException extends RuntimeException {
    public InvalidPasswordResetOtpException(String message) {
        super(message);
    }
}

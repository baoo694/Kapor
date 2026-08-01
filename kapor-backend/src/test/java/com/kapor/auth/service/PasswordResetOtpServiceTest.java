package com.kapor.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Password reset OTP service")
class PasswordResetOtpServiceTest {
    private static final String PREFIX = "kapor:auth:password-reset:";

    @Mock
    private StringRedisTemplate redisTemplate;

    private PasswordResetOtpService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetOtpService(redisTemplate, 15, 60, 5, 60, 5, 15);
    }

    @Test
    @DisplayName("issues a cryptographically generated six-digit OTP")
    void issuesSixDigitOtp() {
        mockIssueResult(1L);

        String otp = service.issueOtp(" User@Example.com ");

        assertThat(otp).matches("\\d{6}");
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(
                        PREFIX + "lock:user@example.com",
                        PREFIX + "send-cooldown:user@example.com",
                        PREFIX + "send-quota:user@example.com",
                        PREFIX + "otp:user@example.com",
                        PREFIX + "attempts:user@example.com")),
                eq("5"), eq("60000"), eq("3600000"), eq(otp), eq("900000"));
    }

    @Test
    @DisplayName("rejects resend attempts during the cooldown")
    void rejectsResendDuringCooldown() {
        mockIssueResult(-1L);
        when(redisTemplate.getExpire(PREFIX + "send-cooldown:user@example.com", TimeUnit.SECONDS))
                .thenReturn(42L);

        assertThatThrownBy(() -> service.issueOtp("user@example.com"))
                .isInstanceOfSatisfying(PasswordResetRateLimitException.class, exception ->
                        assertThat(exception.getRetryAfterSeconds()).isEqualTo(42));
    }

    @Test
    @DisplayName("rejects sends after the hourly quota is exhausted")
    void rejectsExhaustedSendQuota() {
        mockIssueResult(-3L);
        when(redisTemplate.getExpire(PREFIX + "send-quota:user@example.com", TimeUnit.SECONDS))
                .thenReturn(900L);

        assertThatThrownBy(() -> service.issueOtp("user@example.com"))
                .isInstanceOfSatisfying(PasswordResetRateLimitException.class, exception ->
                        assertThat(exception.getRetryAfterSeconds()).isEqualTo(900));
    }

    @Test
    @DisplayName("blocks new sends while verification is locked")
    void rejectsSendDuringVerificationLockout() {
        mockIssueResult(-2L);
        when(redisTemplate.getExpire(PREFIX + "lock:user@example.com", TimeUnit.SECONDS))
                .thenReturn(600L);

        assertThatThrownBy(() -> service.issueOtp("user@example.com"))
                .isInstanceOfSatisfying(PasswordResetRateLimitException.class, exception ->
                        assertThat(exception.getRetryAfterSeconds()).isEqualTo(600));
    }

    @Test
    @DisplayName("accepts and atomically consumes a valid OTP")
    void acceptsValidOtp() {
        mockVerifyResult(0L);

        assertThatCode(() -> service.verifyAndConsume("user@example.com", "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reports remaining attempts after an invalid OTP")
    void tracksInvalidOtpAttempts() {
        mockVerifyResult(2L);

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", "000000"))
                .isInstanceOf(InvalidPasswordResetOtpException.class)
                .hasMessageContaining("3 attempt(s) remaining");
    }

    @Test
    @DisplayName("locks verification after too many invalid attempts")
    void locksAfterTooManyInvalidAttempts() {
        mockVerifyResult(-2L);
        when(redisTemplate.getExpire(PREFIX + "lock:user@example.com", TimeUnit.SECONDS))
                .thenReturn(840L);

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", "000000"))
                .isInstanceOfSatisfying(PasswordResetRateLimitException.class, exception ->
                        assertThat(exception.getRetryAfterSeconds()).isEqualTo(840));
    }

    @Test
    @DisplayName("rejects an expired OTP")
    void rejectsExpiredOtp() {
        mockVerifyResult(-1L);

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", "123456"))
                .isInstanceOf(InvalidPasswordResetOtpException.class)
                .hasMessage("Invalid or expired OTP");
    }

    @Test
    @DisplayName("invalidates OTP state when email delivery fails")
    void invalidatesOtp() {
        service.invalidateOtp("User@Example.com");

        verify(redisTemplate).delete(List.of(
                PREFIX + "otp:user@example.com",
                PREFIX + "attempts:user@example.com"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mockIssueResult(Long result) {
        doReturn(result).when(redisTemplate).execute(
                any(RedisScript.class), anyList(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mockVerifyResult(Long result) {
        doReturn(result).when(redisTemplate).execute(
                any(RedisScript.class), anyList(), any(), any(), any(), any());
    }
}

package com.kapor.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kapor.auth.dto.*;
import com.kapor.auth.security.CustomUserDetails;
import com.kapor.auth.security.JwtService;
import com.kapor.user.dto.UserDto;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        // 1. Verify Google ID token
        GoogleIdToken.Payload payload = googleAuthService.verifyToken(request.getIdToken());
        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        // 2. Find or create user
        Optional<User> userOpt = userRepository.findByEmail(email);
        boolean isNewUser = userOpt.isEmpty();
        
        User user;
        if (isNewUser) {
            user = User.builder()
                    .email(email)
                    .provider("google")
                    .providerId(googleId)
                    .roles(Collections.singleton("ROLE_USER"))
                    .profile(User.Profile.builder()
                            .displayName(name)
                            .avatarUrl(pictureUrl)
                            .joinedAt(Instant.now())
                            .build())
                    .streak(new User.Streak())
                    .settings(new User.UserSettings())
                    .stats(new User.UserStats())
                    .build();
        } else {
            user = userOpt.get();
            // Update profile pic if needed, etc.
        }

        // 3. Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(2592000000L)); // 30 days
        
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .user(UserDto.fromEntity(user))
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("email")
                .providerId(request.getEmail())
                .roles(Collections.singleton("ROLE_USER"))
                .profile(User.Profile.builder()
                        .displayName(request.getName())
                        .joinedAt(Instant.now())
                        .build())
                .streak(new User.Streak())
                .settings(new User.UserSettings())
                .stats(new User.UserStats())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(2592000000L)); // 30 days
        
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(true)
                .user(UserDto.fromEntity(user))
                .build();
    }

    public AuthResponse loginWithEmail(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(2592000000L)); // 30 days
        
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(false)
                .user(UserDto.fromEntity(user))
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"email".equals(user.getProvider())) {
            throw new RuntimeException("This email is registered with a different provider");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        
        // Save OTP to Redis for 15 minutes
        redisTemplate.opsForValue().set("OTP:" + request.getEmail(), otp, 15, TimeUnit.MINUTES);

        // Send actual email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Kapor Support <noreply@kapor.com>");
        message.setTo(request.getEmail());
        message.setSubject("Kapor - Password Reset OTP");
        message.setText("Hello,\n\nYour password reset OTP is: " + otp + "\n\nThis OTP will expire in 15 minutes.\n\nIf you didn't request a password reset, please ignore this email.");
        mailSender.send(message);

        log.info("Email sent to: {}", request.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        String cacheKey = "OTP:" + request.getEmail();
        String savedOtp = redisTemplate.opsForValue().get(cacheKey);

        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete OTP
        redisTemplate.delete(cacheKey);
    }
}

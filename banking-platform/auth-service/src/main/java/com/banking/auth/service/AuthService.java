package com.banking.auth.service;

import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.model.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.model.PasswordResetToken;
import com.banking.auth.repository.PasswordResetTokenRepository;
import com.banking.auth.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailVerificationService emailVerificationService,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       ApplicationEventPublisher eventPublisher) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Register attempt for username={}, email={}", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed — username already taken: {}", request.getUsername());
            throw new IllegalArgumentException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already in use: {}", request.getEmail());
            throw new IllegalArgumentException("Email is already in use.");
        }

        User user = new User(
                UUID.randomUUID().toString(),
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                "ROLE_USER"
        );

        userRepository.save(user);
        log.info("User created successfully: id={}, username={}", user.getId(), user.getUsername());

        String verificationToken = emailVerificationService.createVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                verificationToken
        ));
        log.info("UserRegisteredEvent published for userId={}", user.getId());
    }

    @Transactional
    public void forgotPassword(String email) {
        log.info("Forgot password request for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Forgot password — user not found for email={}", email);
                    return new IllegalArgumentException("User with this email not found.");
                });

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken(
                user.getId(),
                token,
                LocalDateTime.now().plusMinutes(15)
        );

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token created for userId={}", user.getId());

        String resetLink = "http://frontend/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        log.info("Password reset email sent to email={}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Reset password attempt with token={}", token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Reset password — invalid token={}", token);
                    return new IllegalArgumentException("Invalid token.");
                });

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Reset password — token expired or already used: token={}", token);
            throw new IllegalArgumentException("Token is invalid or expired.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset successfully for userId={}", user.getId());
    }
}
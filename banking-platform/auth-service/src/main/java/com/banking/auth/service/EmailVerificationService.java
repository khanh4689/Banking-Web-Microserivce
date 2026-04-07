package com.banking.auth.service;

import com.banking.auth.model.EmailVerificationToken;
import com.banking.auth.model.User;
import com.banking.auth.repository.EmailVerificationTokenRepository;
import com.banking.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository, UserRepository userRepository,
            EmailSender emailSender) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public String createVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user,
                LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);
        log.info("Verification token created for userId={}", user.getId());
        return token;
    }

    @Transactional
    public void resendVerificationToken(String email) {
        log.info("Resend verification token for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Resend verification — user not found for email={}", email);
                    return new IllegalArgumentException("User not found");
                });

        if (user.isEmailVerified()) {
            log.warn("Resend verification — email already verified for email={}", email);
            throw new IllegalArgumentException("Email is already verified");
        }

        String token = createVerificationToken(user);

        String verifyLink = "http://localhost:8082/auth/verify-email?token=" + token;
        String body = "Click the link below to verify your email:\n" + verifyLink;
        emailSender.send(user.getEmail(), "Verify your account", body);
        log.info("Verification email resent to email={}", email);
    }

    @Transactional
    public boolean verifyEmail(String tokenStr) {
        log.info("Verify email with token={}", tokenStr);

        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) {
            log.warn("Verify email — token not found: {}", tokenStr);
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now()) || token.isUsed()) {
            log.warn("Verify email — token expired or already used: {}", tokenStr);
            return false;
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Email verified successfully for userId={}", user.getId());
        return true;
    }
}

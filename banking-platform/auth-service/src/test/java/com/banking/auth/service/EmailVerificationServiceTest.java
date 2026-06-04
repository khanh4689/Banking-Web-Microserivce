package com.banking.auth.service;

import com.banking.auth.model.EmailVerificationToken;
import com.banking.auth.model.User;
import com.banking.auth.repository.EmailVerificationTokenRepository;
import com.banking.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailSender emailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("user-1", "khanh", "khanh@example.com", "hashed", "ROLE_USER");
    }

    // ─── createVerificationToken() ───────────────────────────────────────────

    @Test
    @DisplayName("createVerificationToken: saves token and returns non-null string")
    void createVerificationToken_success() {
        String token = emailVerificationService.createVerificationToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());

        EmailVerificationToken saved = captor.getValue();
        assertEquals(token, saved.getToken());
        assertEquals(user, saved.getUser());
        assertFalse(saved.isUsed());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    // ─── resendVerificationToken() ───────────────────────────────────────────

    @Test
    @DisplayName("resendVerificationToken: success — creates token and sends email")
    void resendVerificationToken_success() {
        when(userRepository.findByEmail("khanh@example.com")).thenReturn(Optional.of(user));

        emailVerificationService.resendVerificationToken("khanh@example.com");

        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailSender).send(eq("khanh@example.com"), eq("Verify your account"), contains("verify-email?token="));
    }

    @Test
    @DisplayName("resendVerificationToken: throws when user not found")
    void resendVerificationToken_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailVerificationService.resendVerificationToken("unknown@example.com"));

        assertEquals("User not found", ex.getMessage());
        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("resendVerificationToken: throws when email already verified")
    void resendVerificationToken_throwsWhenAlreadyVerified() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail("khanh@example.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailVerificationService.resendVerificationToken("khanh@example.com"));

        assertEquals("Email is already verified", ex.getMessage());
        verify(emailSender, never()).send(any(), any(), any());
    }

    // ─── verifyEmail() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyEmail: success — marks user verified and token used")
    void verifyEmail_success() {
        EmailVerificationToken token = new EmailVerificationToken("valid-token", user,
                LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        boolean result = emailVerificationService.verifyEmail("valid-token");

        assertTrue(result);
        assertTrue(user.isEmailVerified());
        assertTrue(user.isEnabled());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyEmail: returns false when token not found")
    void verifyEmail_returnsFalseWhenTokenNotFound() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        boolean result = emailVerificationService.verifyEmail("bad-token");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyEmail: returns false when token expired")
    void verifyEmail_returnsFalseWhenExpired() {
        EmailVerificationToken expiredToken = new EmailVerificationToken("expired-token", user,
                LocalDateTime.now().minusHours(1)); // expired

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        boolean result = emailVerificationService.verifyEmail("expired-token");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyEmail: returns false when token already used")
    void verifyEmail_returnsFalseWhenAlreadyUsed() {
        EmailVerificationToken usedToken = new EmailVerificationToken("used-token", user,
                LocalDateTime.now().plusHours(1));
        usedToken.setUsed(true);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        boolean result = emailVerificationService.verifyEmail("used-token");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }
}

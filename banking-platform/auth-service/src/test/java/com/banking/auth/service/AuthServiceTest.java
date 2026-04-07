package com.banking.auth.service;

import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.event.UserRegisteredEvent;
import com.banking.auth.model.PasswordResetToken;
import com.banking.auth.model.User;
import com.banking.auth.repository.PasswordResetTokenRepository;
import com.banking.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("khanh", "khanh@example.com", "password123");
    }

    // ─── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success — saves user and publishes event")
    void register_success() {
        when(userRepository.existsByUsername("khanh")).thenReturn(false);
        when(userRepository.existsByEmail("khanh@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(emailVerificationService.createVerificationToken(any(User.class))).thenReturn("token-abc");

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).createVerificationToken(any(User.class));

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertEquals("khanh@example.com", event.email());
        assertEquals("khanh", event.username());
        assertEquals("token-abc", event.verificationToken());
    }

    @Test
    @DisplayName("register: throws when username already taken")
    void register_throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("khanh")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken.", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("register: throws when email already in use")
    void register_throwsWhenEmailExists() {
        when(userRepository.existsByUsername("khanh")).thenReturn(false);
        when(userRepository.existsByEmail("khanh@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email is already in use.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ─── forgotPassword() ────────────────────────────────────────────────────

    @Test
    @DisplayName("forgotPassword: success — saves token and sends email")
    void forgotPassword_success() {
        User user = new User("user-1", "khanh", "khanh@example.com", "hashed", "ROLE_USER");
        when(userRepository.findByEmail("khanh@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("khanh@example.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("khanh@example.com"), contains("reset-password?token="));
    }

    @Test
    @DisplayName("forgotPassword: throws when email not found")
    void forgotPassword_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.forgotPassword("unknown@example.com"));

        assertEquals("User with this email not found.", ex.getMessage());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // ─── resetPassword() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: success — updates password and marks token used")
    void resetPassword_success() {
        User user = new User("user-1", "khanh", "khanh@example.com", "old-hash", "ROLE_USER");
        PasswordResetToken resetToken = new PasswordResetToken("user-1", "valid-token",
                LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-hash");

        authService.resetPassword("valid-token", "newPass123");

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(resetToken.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    @DisplayName("resetPassword: throws when token not found")
    void resetPassword_throwsWhenTokenNotFound() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("bad-token", "newPass"));

        assertEquals("Invalid token.", ex.getMessage());
    }

    @Test
    @DisplayName("resetPassword: throws when token is expired")
    void resetPassword_throwsWhenTokenExpired() {
        PasswordResetToken expiredToken = new PasswordResetToken("user-1", "expired-token",
                LocalDateTime.now().minusMinutes(1)); // already expired

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("expired-token", "newPass"));

        assertEquals("Token is invalid or expired.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword: throws when token already used")
    void resetPassword_throwsWhenTokenAlreadyUsed() {
        PasswordResetToken usedToken = new PasswordResetToken("user-1", "used-token",
                LocalDateTime.now().plusMinutes(10));
        usedToken.setUsed(true);

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("used-token", "newPass"));

        assertEquals("Token is invalid or expired.", ex.getMessage());
    }
}

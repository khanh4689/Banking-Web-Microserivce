package com.banking.auth.controller;

import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.LoginResponse;
import com.banking.auth.model.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtProperties;
import com.banking.auth.security.JwtUtil;
import com.banking.auth.service.AuthService;
import com.banking.auth.service.EmailVerificationService;
import com.banking.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks private AuthController authController;

    private User verifiedUser;
    private User unverifiedUser;
    private LoginRequest validRequest;

    @BeforeEach
    void setUp() {
        verifiedUser = new User("user-001", "khanh", "khanh@example.com",
                "$2a$10$hashedpassword", "ROLE_USER");
        verifiedUser.setEnabled(true);
        verifiedUser.setEmailVerified(true);

        unverifiedUser = new User("user-002", "unverified", "unverified@example.com",
                "$2a$10$hashedpassword", "ROLE_USER");
        unverifiedUser.setEnabled(false);
        unverifiedUser.setEmailVerified(false);

        validRequest = new LoginRequest();
        validRequest.setUsername("khanh");
        validRequest.setPassword("password123");
    }

    // ─── login success ────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: success — returns 200 with access and refresh token")
    void login_success() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("khanh");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByUsername("khanh")).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken("user-001", "khanh", List.of("ROLE_USER"))).thenReturn("access-token-xyz");
        when(jwtProperties.getExpirationMinutes()).thenReturn(10L);
        when(refreshTokenService.createRefreshToken(verifiedUser)).thenReturn("refresh-token-xyz");

        ResponseEntity<?> response = authController.login(validRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify token generation was called — password never logged
        verify(jwtUtil).generateToken("user-001", "khanh", List.of("ROLE_USER"));
        verify(refreshTokenService).createRefreshToken(verifiedUser);
    }

    @Test
    @DisplayName("login: success — response contains correct token data")
    void login_success_responseData() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("khanh");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("khanh")).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtProperties.getExpirationMinutes()).thenReturn(10L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        @SuppressWarnings("unchecked")
        var body = (com.banking.auth.dto.ApiResponse<LoginResponse>) authController.login(validRequest).getBody();

        assertNotNull(body);
        assertEquals(200, body.getStatus());
        assertEquals("Login successful", body.getMessage());
        assertNotNull(body.getData());
        assertEquals("access-token", body.getData().getAccessToken());
        assertEquals("refresh-token", body.getData().getRefreshToken());
        assertEquals(600L, body.getData().getExpiresIn());
    }

    // ─── login failure — bad credentials ─────────────────────────────────────

    @Test
    @DisplayName("login: fails with 401 when password is wrong")
    void login_failsWithBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = authController.login(validRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        // Ensure no token was generated
        verify(jwtUtil, never()).generateToken(any(), any(), any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("login: fails with 401 when user not found")
    void login_failsWhenUserNotFound() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        ResponseEntity<?> response = authController.login(new LoginRequest() {{
            setUsername("ghost");
            setPassword("pass");
        }});

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("login: fails with 401 when email not verified")
    void login_failsWhenEmailNotVerified() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("unverified");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("unverified")).thenReturn(Optional.of(unverifiedUser));

        ResponseEntity<?> response = authController.login(new LoginRequest() {{
            setUsername("unverified");
            setPassword("pass");
        }});

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        var body = (com.banking.auth.dto.ApiResponse<?>) response.getBody();
        assertNotNull(body);
        assertEquals("Email not verified", body.getMessage());

        // No token generated for unverified user
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    // ─── security checks ─────────────────────────────────────────────────────

    @Test
    @DisplayName("security: password is never passed to JWT or logged — only userId used")
    void security_passwordNotInToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("khanh");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("khanh")).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");
        when(jwtProperties.getExpirationMinutes()).thenReturn(10L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh");

        authController.login(validRequest);

        // generateToken must receive userId (not password) as first arg
        verify(jwtUtil).generateToken(
                eq("user-001"),   // userId — safe
                eq("khanh"),      // username — safe
                eq(List.of("ROLE_USER"))
        );
        // password "password123" must NEVER appear in any mock call
        verify(jwtUtil, never()).generateToken(eq("password123"), any(), any());
    }

    @Test
    @DisplayName("security: password is encoded — passwordHash starts with bcrypt prefix")
    void security_passwordIsEncoded() {
        assertTrue(verifiedUser.getPasswordHash().startsWith("$2a$"),
                "Password must be BCrypt encoded, not plain text");
    }

    // ─── refresh token ────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh: fails with 400 when token is blank")
    void refresh_failsWhenTokenBlank() {
        var request = new com.banking.auth.dto.RefreshRequest();
        request.setRefreshToken("  ");

        ResponseEntity<?> response = authController.refresh(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("refresh: fails with 401 when token is invalid")
    void refresh_failsWhenTokenInvalid() {
        var request = new com.banking.auth.dto.RefreshRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenService.rotateRefreshToken("invalid-token"))
                .thenThrow(new RuntimeException("Refresh token not found"));

        ResponseEntity<?> response = authController.refresh(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

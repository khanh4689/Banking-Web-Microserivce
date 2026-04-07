package com.banking.auth.controller;

import com.banking.auth.dto.ApiResponse;
import com.banking.auth.dto.ForgotPasswordRequest;
import com.banking.auth.dto.ResetPasswordRequest;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.LoginResponse;
import com.banking.auth.dto.RefreshRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.VerifyEmailRequest;
import com.banking.auth.model.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtProperties;
import com.banking.auth.security.JwtUtil;
import com.banking.auth.service.AuthService;
import com.banking.auth.service.EmailVerificationService;
import com.banking.auth.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    public AuthController(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, JwtProperties jwtProperties,
            AuthenticationManager authenticationManager, AuthService authService,
            UserRepository userRepository, EmailVerificationService emailVerificationService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully. Please verify your email."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @org.springframework.web.bind.annotation.RequestParam("token") String token) {
        boolean verified = emailVerificationService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully."));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Invalid or expired verification token.", "BAD_REQUEST"));
        }
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @RequestBody com.banking.auth.dto.ResendVerificationEmailRequest request) {
        emailVerificationService.resendVerificationToken(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email resent."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow();

            if (!user.isEmailVerified() || !user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "Email not verified", "UNAUTHORIZED"));
            }

            List<String> roles = List.of(user.getRole());
            String accessToken = jwtUtil.generateToken(user.getId(), username, roles);
            long expiresInSeconds = jwtProperties.getExpirationMinutes() * 60L;
            String refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("LOGIN_SUCCESS - userId: {}", user.getId());
            return ResponseEntity.ok(ApiResponse.success("Login successful",
                    new LoginResponse(accessToken, refreshToken, expiresInSeconds)));

        } catch (AuthenticationException e) {
            log.warn("LOGIN_FAILED - email: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Invalid credentials", "UNAUTHORIZED"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshRequest request) {
        String submittedToken = request.getRefreshToken();
        if (submittedToken == null || submittedToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Refresh token is required", "BAD_REQUEST"));
        }

        try {
            String[] result = refreshTokenService.rotateRefreshToken(submittedToken);
            String username = result[0];
            String newRefreshToken = result[1];

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> roles = List.of(user.getRole());
            String newAccessToken = jwtUtil.generateToken(user.getId(), username, roles);
            long expiresInSeconds = jwtProperties.getExpirationMinutes() * 60L;

            return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                    new LoginResponse(newAccessToken, newRefreshToken, expiresInSeconds)));
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Invalid or expired refresh token", "UNAUTHORIZED"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully."));
    }
}

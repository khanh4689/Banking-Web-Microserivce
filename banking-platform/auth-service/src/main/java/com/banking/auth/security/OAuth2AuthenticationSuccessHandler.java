package com.banking.auth.security;

import com.banking.auth.model.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.banking.common.event.UserCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = Optional.ofNullable((String) oAuth2User.getAttribute("email"))
                .map(String::toLowerCase)
                .orElse(null);

        String name = Optional.ofNullable((String) oAuth2User.getAttribute("name"))
                .orElse("user");

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth2 provider did not return email");
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuthUser(email, name));

        List<String> roles = List.of(user.getRole());

        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        long expiresInSeconds = jwtProperties.getExpirationMinutes() * 60L;

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("accessToken", accessToken);
        tokenResponse.put("refreshToken", refreshToken);
        tokenResponse.put("tokenType", "Bearer");
        tokenResponse.put("expiresIn", expiresInSeconds);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), tokenResponse);
    }

    private User createOAuthUser(String email, String name) {

        String username = generateUsername(email);

        User user = new User(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                DEFAULT_ROLE
        );

        user.setEmailVerified(true);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername()
        );
        eventPublisher.publishEvent(event);

        return savedUser;
    }

    private String generateUsername(String email) {

        String baseUsername = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "");

        String username = baseUsername;
        int attempt = 0;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 5);
            attempt++;

            if (attempt > 5) {
                username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }

        return username;
    }
}
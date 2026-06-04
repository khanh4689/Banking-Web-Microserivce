package com.banking.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler successHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .csrf(csrf -> csrf.disable())

        .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/verify-email",
                    "/auth/refresh",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/auth/oauth2/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/.well-known/jwks.json",
                    "/actuator/**",
                    "/error"
            ).permitAll()
            .anyRequest().authenticated()
        )

        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(endpoint ->
                    endpoint.baseUri("/auth/oauth2"))
            .redirectionEndpoint(endpoint ->
                    endpoint.baseUri("/login/oauth2/code/*"))
            .successHandler(successHandler)
        )

        // 🔥 QUAN TRỌNG: đặt SAU oauth2Login
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, e) -> {
                res.sendError(401);
            })
        )

        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable());

    return http.build();
}
}
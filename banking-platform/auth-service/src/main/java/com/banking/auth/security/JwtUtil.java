package com.banking.auth.security;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final KeyPair rsaKeyPair;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.rsaKeyPair = generateRsaKey();
    }

    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not supported", ex);
        }
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) this.rsaKeyPair.getPublic();
    }

    private RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) this.rsaKeyPair.getPrivate();
    }

    /**
     * @param userId   the user's UUID — used as JWT subject so user-service can lookup by authId
     * @param username stored as a claim for display purposes
     * @param roles    list of roles
     */
    public String generateToken(String userId, String username, List<String> roles) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (jwtProperties.getExpirationMinutes() * 60 * 1000);
        Date now = new Date(nowMillis);
        Date exp = new Date(expMillis);

        String token = Jwts.builder()
                .subject(userId)           // sub = UUID → used by user-service as authId
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getPrivateKey())
                .compact();
        log.info("TOKEN_GENERATED - userId: {}", userId);
        return token;
    }
}

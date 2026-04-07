package com.banking.auth.service;

import com.banking.auth.security.HashUtil;
import com.banking.auth.model.RefreshToken;
import com.banking.auth.model.User;
import com.banking.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the user.
     * 
     * @param user The database user
     * @return The raw (unhashed) random 256-bit token.
     */
    public String createRefreshToken(User user) {
        String rawToken = HashUtil.generateSecureToken();
        String tokenHash = HashUtil.hashWithSHA256(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        // 7 Days expiration
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Rotates a given active refresh token.
     * Validates expiration and revocation status.
     * If the submitted token was ALREADY revoked -> reuse detection triggers,
     * revoking ALL of this user's tokens.
     * 
     * @param rawToken the unhashed token the client sends.
     * @return String[] {username, newRawToken}
     */
    @Transactional
    public String[] rotateRefreshToken(String rawToken) {
        String tokenHash = HashUtil.hashWithSHA256(rawToken);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (oldToken.isRevoked()) {
            // REUSE DETECTED: Revoke all active tokens for this user immediately
            refreshTokenRepository.deleteByUser(oldToken.getUser());
            throw new RuntimeException(
                    "Refresh token reuse detected. All tokens revoked for user: " + oldToken.getUser().getUsername());
        }

        if (oldToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        // Valid rotation: Mark old as revoked
        oldToken.setRevoked(true);

        // Generate new token pair
        String newRawToken = HashUtil.generateSecureToken();
        String newTokenHash = HashUtil.hashWithSHA256(newRawToken);

        oldToken.setReplacedByToken(newTokenHash);
        refreshTokenRepository.save(oldToken);

        // Save new active token
        RefreshToken newToken = new RefreshToken();
        newToken.setUser(oldToken.getUser());
        newToken.setTokenHash(newTokenHash);
        newToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(newToken);

        return new String[] { oldToken.getUser().getUsername(), newRawToken };
    }
}

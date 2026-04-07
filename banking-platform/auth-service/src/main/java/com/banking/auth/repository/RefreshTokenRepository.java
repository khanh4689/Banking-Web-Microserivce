package com.banking.auth.repository;

import com.banking.auth.model.RefreshToken;
import com.banking.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // To revoke all active tokens for a given user upon reuse detection
    void deleteByUser(User user);

}

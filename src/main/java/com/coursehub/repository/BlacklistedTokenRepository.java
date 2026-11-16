package com.coursehub.repository;

import com.coursehub.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByToken(String token);
    // Removes tokens that have expired anyway, so the table stays small.
    void deleteByExpiresAtBefore(LocalDateTime time);
}

package com.snor.quotaguard.auth.repository;

import com.snor.quotaguard.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    List<RefreshToken> findAllByExpiresAtBefore(Instant now);

    void deleteAllByExpiresAtBefore(Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true "
            + "WHERE t.familyId = :familyId AND t.revoked = false")
    int revokeFamily(@Param("familyId") UUID familyId);
}

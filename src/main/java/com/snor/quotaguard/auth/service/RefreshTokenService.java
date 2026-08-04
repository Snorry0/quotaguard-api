package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.auth.domain.RefreshToken;
import com.snor.quotaguard.auth.exception.InvalidRefreshTokenException;
import com.snor.quotaguard.auth.repository.RefreshTokenRepository;
import com.snor.quotaguard.config.JwtProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Sole owner of refresh-token generation, hashing, rotation and revocation.
 *
 * <p>Raw tokens are opaque 256-bit SecureRandom values returned to the client exactly once;
 * only their SHA-256 hex hashes are persisted. Rotation is single-use: the presented token is
 * revoked and a new token is issued within the same token family in one transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final DomainEventPublisher domainEventPublisher; // injected for future use; NOT called
    private final Clock clock;

    /** Issue a new refresh token for the given user (new family = new device). */
    public IssuedRefreshToken issue(User user) {
        return issueInFamily(user, UUID.randomUUID());
    }

    /** Issue a new refresh token within an existing family (for rotation). */
    public IssuedRefreshToken issueInFamily(User user, UUID familyId) {
        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(user.getId())
                .familyId(familyId)
                .createdAt(Instant.now(clock))
                .expiresAt(Instant.now(clock).plus(jwtProperties.refreshExpiration()))
                .revoked(false)
                .lastUsedAt(null)
                .build();
        RefreshToken saved = refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(saved, rawToken);
    }

    /**
     * Rotate a presented refresh token: validate it, revoke the old token and issue a new one
     * in the same family and transaction.
     *
     * @throws InvalidRefreshTokenException on not-found, revoked, expired or replayed tokens
     *                                      (generic 401; the real reason is logged).
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshedSession rotate(String presentedRawToken) {
        String tokenHash = sha256Hex(presentedRawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("not found"));
        Instant now = Instant.now(clock);
        if (existing.isRevoked() || !existing.isUsable(now)) {
            // Replay detection: revoke the whole family (Auth0 model) + generic 401
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            log.warn("Refresh token reuse detected: family={} user={}", existing.getFamilyId(), existing.getUserId());
            throw new InvalidRefreshTokenException("revoked or expired");
        }
        try {
            existing.revoke();
            existing.touchUsed(now);
            refreshTokenRepository.save(existing); // @Version handles concurrent rotation
        } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException ex) {
            // F3 fix: concurrent rotation → generic 401 (not 409)
            log.warn("Concurrent refresh token rotation: family={} user={}", existing.getFamilyId(), existing.getUserId());
            throw new InvalidRefreshTokenException("concurrent rotation");
        }
        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("user not found"));
        IssuedRefreshToken newIssued = issueInFamily(user, existing.getFamilyId()); // same family → rotation within lineage
        String newAccessToken = jwtService.generateToken(user);
        Instant newExpiresAt = jwtService.getExpirationInstant();
        return new RefreshedSession(newAccessToken, newIssued.rawToken(), newExpiresAt, user);
    }

    /** Revoke the presented refresh token. Idempotent — unknown tokens are a no-op. */
    @Transactional
    public void revoke(String presentedRawToken) {
        String tokenHash = sha256Hex(presentedRawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            token.touchUsed(Instant.now(clock));
            refreshTokenRepository.save(token);
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /** Internal result of issuing a token: the persisted entity + the raw value returned to the client. */
    public record IssuedRefreshToken(RefreshToken entity, String rawToken) {}

    /** Result of a successful rotation: a fresh access token + a new refresh token. */
    public record RefreshedSession(String accessToken, String newRefreshToken, Instant expiresAt, User user) {}
}

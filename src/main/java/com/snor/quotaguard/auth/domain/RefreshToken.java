package com.snor.quotaguard.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A long-lived refresh token credential.
 *
 * <p>{@code tokenHash} stores the hex-encoded SHA-256 hash of the raw token (the raw
 * value is returned to the client once and never persisted). {@code familyId} groups
 * the rotation lineage for a single device, so a replayed token revokes that device's
 * whole family without affecting other devices.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public void revoke() {
        this.revoked = true;
    }

    public void touchUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public boolean isUsable(Instant now) {
        return !revoked && now.isBefore(expiresAt);
    }
}

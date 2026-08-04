package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.auth.domain.RefreshToken;
import com.snor.quotaguard.auth.exception.InvalidRefreshTokenException;
import com.snor.quotaguard.auth.repository.RefreshTokenRepository;
import com.snor.quotaguard.config.JwtProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.repository.UserRepository;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private static final String RAW_TOKEN_PATTERN = "^[A-Za-z0-9_-]{43}$";
    private static final String HASH_PATTERN = "^[0-9a-f]{64}$";
    private static final String RAW_TOKEN = "k2v9Xm4Pq8Lz1Rt7Wb3Yd6Nc0Je5Af2Hg8Ij4Kl6Mn3Op";
    private static final UUID USER_ID = UUID.fromString("2f07c5b2-4f0d-4090-86c1-021e5f6b80f8");

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void issueIssuesNewFamily() {
        TestHarness h = harness();
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);

        RefreshTokenService.IssuedRefreshToken issued = h.service().issue(user);

        assertThat(issued.rawToken()).hasSize(43).matches(RAW_TOKEN_PATTERN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(h.refreshTokenRepository()).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(issued.entity()).isSameAs(saved);
        assertThat(saved.getTokenHash()).hasSize(64).matches(HASH_PATTERN);
        assertThat(saved.getTokenHash()).isEqualTo(sha256Hex(issued.rawToken()));
        assertThat(saved.getTokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(saved.getFamilyId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(saved.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(1)));
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getLastUsedAt()).isNull();
    }

    @Test
    void issueInFamilyKeepsFamilyId() {
        TestHarness h = harness();
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        UUID familyId = UUID.randomUUID();

        h.service().issueInFamily(user, familyId);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(h.refreshTokenRepository()).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void rotateHappyPath() {
        TestHarness h = harness();
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(h.userRepository().findById(USER_ID)).thenReturn(Optional.of(user));
        when(h.jwtService().generateToken(user)).thenReturn("new-access-token");
        Instant newExpiresAt = clock.instant().plus(Duration.ofMinutes(15));
        when(h.jwtService().getExpirationInstant()).thenReturn(newExpiresAt);

        RefreshTokenService.RefreshedSession session = h.service().rotate(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(h.refreshTokenRepository(), times(2)).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        RefreshToken oldSaved = saved.get(0);
        RefreshToken newSaved = saved.get(1);

        assertThat(oldSaved.isRevoked()).isTrue();
        assertThat(oldSaved.getLastUsedAt()).isEqualTo(clock.instant());
        assertThat(newSaved.getFamilyId()).isEqualTo(familyId);
        assertThat(newSaved.isRevoked()).isFalse();
        assertThat(newSaved.getLastUsedAt()).isNull();
        assertThat(newSaved.getTokenHash()).isEqualTo(sha256Hex(session.newRefreshToken()));

        assertThat(session.accessToken()).isEqualTo("new-access-token");
        assertThat(session.expiresAt()).isEqualTo(newExpiresAt);
        assertThat(session.user()).isSameAs(user);
        assertThat(session.newRefreshToken()).hasSize(43).matches(RAW_TOKEN_PATTERN);

        verify(h.refreshTokenRepository(), never()).revokeFamily(any());
    }

    @Test
    void rotateRejectsNotFound() {
        TestHarness h = harness();
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> h.service().rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(h.refreshTokenRepository(), never()).revokeFamily(any());
        verify(h.refreshTokenRepository(), never()).save(any());
    }

    @Test
    void rotateRejectsRevoked() {
        TestHarness h = harness();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        existing.revoke();
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> h.service().rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(h.refreshTokenRepository()).revokeFamily(familyId);
        verify(h.refreshTokenRepository(), never()).save(any());
    }

    @Test
    void rotateRejectsExpired() {
        TestHarness h = harness();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        existing.setExpiresAt(clock.instant().minusSeconds(60));
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> h.service().rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(h.refreshTokenRepository()).revokeFamily(familyId);
        verify(h.refreshTokenRepository(), never()).save(any());
    }

    @Test
    void rotateCatchesOptimisticLock() {
        TestHarness h = harness();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(h.refreshTokenRepository().save(existing))
                .thenThrow(new ObjectOptimisticLockingFailureException(
                        RefreshToken.class,
                        existing.getId(),
                        "stale version",
                        new RuntimeException("boom")
                ));

        assertThatThrownBy(() -> h.service().rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(h.refreshTokenRepository(), never()).save(argThat(token -> !token.isRevoked()));
    }

    @Test
    void rotateCatchesStaleObjectState() {
        TestHarness h = harness();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(h.refreshTokenRepository().save(existing))
                .thenThrow(new StaleObjectStateException("RefreshToken", existing.getId()));

        assertThatThrownBy(() -> h.service().rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(h.refreshTokenRepository(), never()).save(argThat(token -> !token.isRevoked()));
    }

    @Test
    void revokeHappyPath() {
        TestHarness h = harness();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        h.service().revoke(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(h.refreshTokenRepository()).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
        assertThat(captor.getValue().getLastUsedAt()).isEqualTo(clock.instant());
    }

    @Test
    void revokeIsIdempotent() {
        TestHarness h = harness();
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.empty());

        h.service().revoke(RAW_TOKEN);

        verify(h.refreshTokenRepository(), never()).save(any());
    }

    @Test
    void rotateGeneratesNewAccessTokenViaJwtService() {
        TestHarness h = harness();
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = validToken(familyId);
        when(h.refreshTokenRepository().findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(h.userRepository().findById(USER_ID)).thenReturn(Optional.of(user));
        when(h.jwtService().generateToken(user)).thenReturn("new-access-token");
        when(h.jwtService().getExpirationInstant()).thenReturn(clock.instant().plus(Duration.ofMinutes(15)));

        h.service().rotate(RAW_TOKEN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(h.jwtService()).generateToken(captor.capture());
        assertThat(captor.getValue()).isSameAs(user);
    }

    private RefreshToken validToken(UUID familyId) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .userId(USER_ID)
                .familyId(familyId)
                .createdAt(clock.instant())
                .expiresAt(clock.instant().plus(Duration.ofHours(1)))
                .revoked(false)
                .lastUsedAt(null)
                .version(0)
                .build();
    }

    private TestHarness harness() {
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        when(jwtProperties.refreshExpiration()).thenReturn(Duration.ofHours(1));
        // Persist-through: a real repository returns the managed entity from save(), so the
        // issued token's entity carries the saved row.
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository,
                userRepository,
                jwtService,
                jwtProperties,
                domainEventPublisher,
                clock
        );
        return new TestHarness(
                service,
                refreshTokenRepository,
                userRepository,
                jwtService,
                jwtProperties,
                domainEventPublisher
        );
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private record TestHarness(
            RefreshTokenService service,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService,
            JwtProperties jwtProperties,
            DomainEventPublisher domainEventPublisher
    ) {
    }
}

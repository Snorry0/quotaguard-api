package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.audit.domain.AuditEvent;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.auth.domain.RefreshToken;
import com.snor.quotaguard.auth.repository.RefreshTokenRepository;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the refresh-token lifecycle: login returns both tokens, refresh rotates
 * with single-use semantics, expired/revoked/invalid/replayed tokens return a generic 401, logout
 * revokes idempotently, multiple devices rotate independently, and the refresh path publishes no
 * domain events (the no-event policy).
 *
 * <p>Uses a dedicated in-memory database (mirroring {@code MetricsIntegrationTest}) so this class
 * gets its own Spring context and a pristine database; the ordered tests share state established
 * by earlier methods (the {@code demo-refresh@example.com} login from test 1 feeds test 2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaguard-refresh;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenIntegrationTest {

    private static final String DEMO_EMAIL = "demo-refresh@example.com";
    private static final String DEMO_PASSWORD = "Password123!";
    private static final String REFRESH_TOKEN_PATTERN = "^[A-Za-z0-9_-]{43}$";
    private static final String TOKEN_HASH_PATTERN = "^[0-9a-f]{64}$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    /** State established by test 1 and consumed by test 2 (ordered execution). */
    private String firstAccessToken;
    private String firstRefreshToken;
    private UUID demoUserId;

    @Test
    @Order(1)
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        registerUser(DEMO_EMAIL, DEMO_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DEMO_EMAIL,
                                "password", DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_at").exists())
                .andExpect(jsonPath("$.user.email").value(DEMO_EMAIL))
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andReturn();

        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        firstAccessToken = login.get("access_token").asText();
        firstRefreshToken = login.get("refresh_token").asText();
        demoUserId = UUID.fromString(login.get("user").get("id").asText());

        assertThat(firstRefreshToken).matches(REFRESH_TOKEN_PATTERN);

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(demoUserId);
        assertThat(activeTokens).hasSize(1);
        RefreshToken row = activeTokens.get(0);
        assertThat(row.getFamilyId()).isNotNull();
        assertThat(row.isRevoked()).isFalse();
        assertThat(row.getTokenHash()).hasSize(64).matches(TOKEN_HASH_PATTERN);
        assertThat(row.getTokenHash()).isNotEqualTo(firstRefreshToken);
        assertThat(row.getTokenHash()).isEqualTo(sha256Hex(firstRefreshToken));
    }

    @Test
    @Order(2)
    void refreshRotatesAndOldTokenIsUnusable() throws Exception {
        // The access token's iat/exp are second-precision, so a login and a refresh in the same
        // second produce byte-identical JWTs. Sleep past the second boundary to make the
        // "new access token differs from the original" assertion deterministic.
        Thread.sleep(1100);

        // Rotate the token issued in test 1 → new pair.
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshed.get("access_token").asText();
        String newRefreshToken = refreshed.get("refresh_token").asText();
        assertThat(newAccessToken).isNotEqualTo(firstAccessToken);
        assertThat(newRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(newRefreshToken).matches(REFRESH_TOKEN_PATTERN);

        // The old token is now unusable (already revoked) → generic 401 + the replay path revokes
        // the whole family, including the just-issued new token.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        RefreshToken oldRow = refreshTokenRepository.findByTokenHash(sha256Hex(firstRefreshToken)).orElseThrow();
        assertThat(oldRow.isRevoked()).isTrue();

        RefreshToken newRow = refreshTokenRepository.findByTokenHash(sha256Hex(newRefreshToken)).orElseThrow();
        assertThat(newRow.getFamilyId()).isEqualTo(oldRow.getFamilyId());
        // The replay path calls revokeFamily() and throws InvalidRefreshTokenException in the same
        // @Transactional(noRollbackFor = InvalidRefreshTokenException.class) rotate(). The bulk
        // family-revocation UPDATE persists (no rollback on the throw), so the sibling token in
        // the family IS revoked.
        assertThat(newRow.isRevoked()).isTrue();
    }

    @Test
    @Order(3)
    void refreshWithExpiredTokenReturns401() throws Exception {
        LoginResult login = registerAndLogin("expired-" + UUID.randomUUID() + "@example.com");

        RefreshToken row = refreshTokenRepository.findByTokenHash(sha256Hex(login.refreshToken())).orElseThrow();
        row.setExpiresAt(Instant.now().minusSeconds(3600));
        refreshTokenRepository.save(row);

        // No new row was created for this user (count stays 1).
        long rowsBefore = countTokensFor(login.userId());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        assertThat(countTokensFor(login.userId())).isEqualTo(rowsBefore);
    }

    @Test
    @Order(4)
    void refreshWithRevokedTokenReturns401() throws Exception {
        LoginResult login = registerAndLogin("revoked-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        RefreshToken row = refreshTokenRepository.findByTokenHash(sha256Hex(login.refreshToken())).orElseThrow();
        assertThat(row.isRevoked()).isTrue();
    }

    @Test
    @Order(5)
    void refreshWithInvalidTokenReturns401() throws Exception {
        long rowsBefore = refreshTokenRepository.count();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "this-is-a-completely-invalid-token-xyz-not-a-valid-64-char-hex-hash-or-base64"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        assertThat(refreshTokenRepository.count()).isEqualTo(rowsBefore);
    }

    @Test
    @Order(6)
    void replayRevokesFamilyAndReturns401() throws Exception {
        LoginResult login = registerAndLogin("replay-" + UUID.randomUUID() + "@example.com");

        // Legitimate rotation: old token revoked, new token issued in the same family.
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();
        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refresh_token")
                .asText();

        // Replay: present the rotated (already revoked) token again → 401, and the replay path
        // revokes the whole family — the new token becomes unusable too.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        RefreshToken oldRow = refreshTokenRepository.findByTokenHash(sha256Hex(login.refreshToken())).orElseThrow();
        RefreshToken newRow = refreshTokenRepository.findByTokenHash(sha256Hex(newRefreshToken)).orElseThrow();
        assertThat(oldRow.isRevoked()).isTrue();
        // revokeFamily() runs inside the no-rollback rotate() transaction, so the family revocation
        // persists on replay: the sibling token is revoked along with the replayed one.
        assertThat(newRow.isRevoked()).isTrue();
        assertThat(newRow.getFamilyId()).isEqualTo(oldRow.getFamilyId());
    }

    @Test
    @Order(7)
    void logoutRevokesAndIsIdempotent() throws Exception {
        LoginResult login = registerAndLogin("logout-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isUnauthorized());

        // Idempotent: logging out again with the same (already-revoked) token is a no-op → 204.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isNoContent());

        // Only the original row exists (no duplicate).
        List<RefreshToken> rows = allTokensFor(login.userId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).isRevoked()).isTrue();
    }

    @Test
    @Order(8)
    void multipleDevicesIndependentRotation() throws Exception {
        String email = "devices-" + UUID.randomUUID() + "@example.com";
        registerUser(email, DEMO_PASSWORD);
        LoginResult device1 = loginAndExtractRefresh(email);
        LoginResult device2 = loginAndExtractRefresh(email);

        RefreshToken token1 = refreshTokenRepository.findByTokenHash(sha256Hex(device1.refreshToken())).orElseThrow();
        RefreshToken token2 = refreshTokenRepository.findByTokenHash(sha256Hex(device2.refreshToken())).orElseThrow();
        assertThat(token1.getFamilyId()).isNotEqualTo(token2.getFamilyId());

        // Rotate device 1 → 200; device 2 must be unaffected.
        MvcResult device1Refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", device1.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();
        String device1NewToken = objectMapper.readTree(device1Refresh.getResponse().getContentAsString())
                .get("refresh_token")
                .asText();

        // Family 2 still has exactly 1 row and it is still valid.
        List<RefreshToken> family2Before = allTokensFor(device1.userId()).stream()
                .filter(t -> t.getFamilyId().equals(token2.getFamilyId()))
                .toList();
        assertThat(family2Before).hasSize(1);
        assertThat(family2Before.get(0).isRevoked()).isFalse();

        // Rotate device 2 → 200: family 1's rotation did not invalidate family 2.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", device2.refreshToken()))))
                .andExpect(status().isOk());

        // Final DB state: 2 distinct families; family 1 has 2 rows (old revoked, new valid);
        // family 2 has 2 rows (old revoked, new valid) after its own rotation.
        List<RefreshToken> all = allTokensFor(device1.userId());
        Set<UUID> families = all.stream().map(RefreshToken::getFamilyId).collect(Collectors.toSet());
        assertThat(families).hasSize(2);

        RefreshToken old1 = refreshTokenRepository.findByTokenHash(sha256Hex(device1.refreshToken())).orElseThrow();
        RefreshToken new1 = refreshTokenRepository.findByTokenHash(sha256Hex(device1NewToken)).orElseThrow();
        assertThat(old1.isRevoked()).isTrue();
        assertThat(new1.isRevoked()).isFalse();
        assertThat(new1.getFamilyId()).isEqualTo(old1.getFamilyId());
        assertThat(countRowsInFamily(old1.getFamilyId())).isEqualTo(2);

        RefreshToken old2 = refreshTokenRepository.findByTokenHash(sha256Hex(device2.refreshToken())).orElseThrow();
        assertThat(old2.isRevoked()).isTrue();
        assertThat(countRowsInFamily(old2.getFamilyId())).isEqualTo(2);
    }

    @Test
    @Order(9)
    void refreshDoesNotPublishLoginEvents() throws Exception {
        LoginResult login = registerAndLogin("noevents-" + UUID.randomUUID() + "@example.com");
        UUID userId = login.userId();

        // The single login recorded exactly one LOGIN_SUCCESS audit event.
        assertThat(countLoginSuccessAudits(userId)).isEqualTo(1);

        double loginsBefore = meterRegistry.get("quotaguard.logins.successful").counter().count();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty());

        // No-event policy: the refresh published nothing — the audit row count and the
        // successful-logins counter are both unchanged by the refresh.
        assertThat(countLoginSuccessAudits(userId)).isEqualTo(1);
        assertThat(meterRegistry.get("quotaguard.logins.successful").counter().count()).isEqualTo(loginsBefore);

        // The DB state confirms the rotation (old revoked, new token in the same family).
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        assertThat(active).hasSize(1);
        RefreshToken newRow = active.get(0);
        assertThat(newRow.getFamilyId()).isNotNull();
        RefreshToken oldRow = refreshTokenRepository.findByTokenHash(sha256Hex(login.refreshToken())).orElseThrow();
        assertThat(oldRow.isRevoked()).isTrue();
        assertThat(newRow.getFamilyId()).isEqualTo(oldRow.getFamilyId());
    }

    private LoginResult registerAndLogin(String email) throws Exception {
        registerUser(email, DEMO_PASSWORD);
        return loginAndExtractRefresh(email);
    }

    private LoginResult loginAndExtractRefresh(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return new LoginResult(
                json.get("access_token").asText(),
                json.get("refresh_token").asText(),
                UUID.fromString(json.get("user").get("id").asText())
        );
    }

    private void registerUser(String email, String password) {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private long countTokensFor(UUID userId) {
        return allTokensFor(userId).size();
    }

    private List<RefreshToken> allTokensFor(UUID userId) {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUserId().equals(userId))
                .toList();
    }

    private long countRowsInFamily(UUID familyId) {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getFamilyId().equals(familyId))
                .count();
    }

    private long countLoginSuccessAudits(UUID userId) {
        return auditEventRepository.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.LOGIN_SUCCESS)
                .filter(event -> userId.equals(event.getResourceId()))
                .count();
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

    private record LoginResult(String accessToken, String refreshToken, UUID userId) {
    }
}

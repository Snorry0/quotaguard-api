package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the web-layer rate limiter on the auth endpoints
 * (test-profile capacities: login 3/min, register 100/hour, refresh 5/min).
 *
 * <p>Each test uses a unique {@code remoteAddr} (the {@code ip} processor) so
 * the shared default IP does not leak bucket state across the ordered class.
 * Uses a dedicated in-memory database (mirroring {@code RefreshTokenIntegrationTest})
 * so this class gets its own Spring context and a pristine bucket store.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaguard-ratelimit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        // Tight capacities for this class (inline properties override the generous
        // application-test.yml values): login 3/min, register 100/hour, refresh 5/min.
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/login].tokens=3",
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/login].refill-period=1m",
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/register].tokens=100",
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/register].refill-period=1h",
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/refresh].tokens=5",
        "quotaguard.rate-limiting.endpoints.[/api/v1/auth/refresh].refill-period=1m"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Order(1)
    void anonymousLoginLimitedByIpReturns429() throws Exception {
        String email = uniqueEmail("ratelimit-anon");
        seedUser(email);

        // The first 3 logins from the same IP succeed (capacity 3/min)…
        for (int i = 0; i < 3; i++) {
            assertThat(postLogin(email, "203.0.113.10").getResponse().getStatus()).isEqualTo(200);
        }
        // …the 4th is rejected even with correct credentials.
        MvcResult fourth = postLogin(email, "203.0.113.10");
        assertThat(fourth.getResponse().getStatus()).isEqualTo(429);
        assertTooManyRequests(fourth, "/api/v1/auth/login");
    }

    @Test
    @Order(2)
    void differentIpsHaveIndependentBuckets() throws Exception {
        String email = uniqueEmail("ratelimit-ips");
        seedUser(email);

        for (int i = 0; i < 3; i++) {
            assertThat(postLogin(email, "203.0.113.20").getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(postLogin(email, "203.0.113.20").getResponse().getStatus()).isEqualTo(429);

        // A fresh IP has its own bucket — unaffected by IP A's exhaustion.
        assertThat(postLogin(email, "203.0.113.21").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    void authenticatedRequestUsesUserKey() throws Exception {
        String email = uniqueEmail("ratelimit-user");
        MvcResult register = postRegister(email, "203.0.113.29");
        assertThat(register.getResponse().getStatus()).isEqualTo(201);
        String accessToken = objectMapper.readTree(register.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token")
                .asText();

        // Same Bearer (the JWT binds the key to USER:<userId>) but different IPs:
        // the per-user bucket is shared across IPs, so the 4th request is rejected
        // even though its IP is fresh.
        List<String> ips = List.of("203.0.113.30", "203.0.113.31", "203.0.113.32");
        for (String ipAddr : ips) {
            assertThat(postLogin(email, ipAddr, accessToken).getResponse().getStatus()).isEqualTo(200);
        }
        MvcResult fourth = postLogin(email, "203.0.113.33", accessToken);
        assertThat(fourth.getResponse().getStatus()).isEqualTo(429);
        assertTooManyRequests(fourth, "/api/v1/auth/login");
    }

    @Test
    @Order(4)
    void refreshLimitedByIp() throws Exception {
        String email = uniqueEmail("ratelimit-refresh");
        MvcResult register = postRegister(email, "203.0.113.39");
        assertThat(register.getResponse().getStatus()).isEqualTo(201);
        String refreshToken = objectMapper.readTree(register.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("refresh_token")
                .asText();

        // 5 refreshes from the same IP succeed (capacity 5/min); each rotates the token.
        for (int i = 0; i < 5; i++) {
            MvcResult refreshed = postRefresh(refreshToken, "203.0.113.40");
            assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
            refreshToken = objectMapper.readTree(refreshed.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .get("refresh_token")
                    .asText();
        }

        MvcResult sixth = postRefresh(refreshToken, "203.0.113.40");
        assertThat(sixth.getResponse().getStatus()).isEqualTo(429);
        assertTooManyRequests(sixth, "/api/v1/auth/refresh");
    }

    @Test
    @Order(5)
    void registerLimitedByIp() throws Exception {
        // 100 registrations from the same IP succeed (capacity 100/hour)…
        for (int i = 0; i < 100; i++) {
            assertThat(postRegister(uniqueEmail("ratelimit-reg-" + i), "203.0.113.50")
                    .getResponse().getStatus()).isEqualTo(201);
        }
        // …the 101st is rejected (each HTTP attempt consumes a token pre-controller).
        MvcResult last = postRegister(uniqueEmail("ratelimit-reg-100"), "203.0.113.50");
        assertThat(last.getResponse().getStatus()).isEqualTo(429);
        assertTooManyRequests(last, "/api/v1/auth/register");
    }

    private RequestPostProcessor ip(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private MvcResult postLogin(String email, String ipAddr) throws Exception {
        return postLogin(email, ipAddr, null);
    }

    private MvcResult postLogin(String email, String ipAddr, String accessToken) throws Exception {
        MockHttpServletRequestBuilder builder = post("/api/v1/auth/login")
                .with(ip(ipAddr))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", PASSWORD)));
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return mockMvc.perform(builder).andReturn();
    }

    private MvcResult postRegister(String email, String ipAddr) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .with(ip(ipAddr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD))))
                .andReturn();
    }

    private MvcResult postRefresh(String refreshToken, String ipAddr) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(ip(ipAddr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken))))
                .andReturn();
    }

    private void assertTooManyRequests(MvcResult result, String expectedPath) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        String retryAfter = result.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isGreaterThanOrEqualTo(1);

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.get("message").asText()).contains("Too many requests");
        assertThat(body.get("path").asText()).isEqualTo(expectedPath);
        assertThat(body.get("validationErrors").get("retryAfterSeconds")).isNotNull();
    }

    private void seedUser(String email) {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}

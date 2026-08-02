package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.user.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the observability feature: the health endpoint, the metrics
 * endpoint, and the business metric increments (counters, timers, the active-sessions
 * gauge).
 *
 * <p>Uses a dedicated in-memory database (mirroring {@code AuditIsolationIntegrationTest})
 * so this class gets its own Spring context and a pristine {@link MeterRegistry} —
 * otherwise the counters would accumulate across the shared test context. The test
 * methods are ordered because later methods depend on state established by earlier
 * ones (registered user, JWTs, session id).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaguard-metrics;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetricsIntegrationTest {

    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private UUID sessionId;

    @Test
    @Order(1)
    void healthIsUpAndShowsAllComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.diskSpace.status").value("UP"))
                .andExpect(jsonPath("$.components.quotaGuardConfig.status").value("UP"));
    }

    @Test
    @Order(2)
    void successfulRegistrationIncrementsCounterAndTimer() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DEMO_EMAIL,
                                "password", DEMO_PASSWORD
                        ))))
                .andExpect(status().isCreated());

        assertThat(meterRegistry.get("quotaguard.registrations.successful").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("quotaguard.timer.registration").timer().count()).isEqualTo(1L);
    }

    @Test
    @Order(3)
    void failedRegistrationIncrementsCounter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DEMO_EMAIL,
                                "password", DEMO_PASSWORD
                        ))))
                .andExpect(status().isConflict());

        assertThat(meterRegistry.get("quotaguard.registrations.failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @Order(4)
    void successfulLoginIncrementsCounterAndTimer() throws Exception {
        userToken = loginAndExtractToken(DEMO_EMAIL, DEMO_PASSWORD);

        assertThat(meterRegistry.get("quotaguard.logins.successful").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("quotaguard.timer.login").timer().count()).isEqualTo(1L);
    }

    @Test
    @Order(5)
    void failedLoginIncrementsCounter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DEMO_EMAIL,
                                "password", "WrongPassword123!"
                        ))))
                .andExpect(status().isUnauthorized());

        assertThat(meterRegistry.get("quotaguard.logins.failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @Order(6)
    void quotaConsumptionIncrementsCounterAndTimer() throws Exception {
        mockMvc.perform(post("/api/v1/usage/consume")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountConsumed", 10,
                                "actionType", "API_CALL"
                        ))))
                .andExpect(status().isOk());

        assertThat(meterRegistry.get("quotaguard.quota.consumptions")
                .tag("actionType", "API_CALL")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("quotaguard.timer.quota.consumption").timer().count()).isEqualTo(1L);
    }

    @Test
    @Order(7)
    void quotaExceededAppliesPenalty() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/usage/consume")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountConsumed", 150,
                                "actionType", "API_CALL"
                        ))))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        String penaltyType = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("validationErrors")
                .get("penaltyType")
                .asText();
        assertThat(PenaltyType.valueOf(penaltyType)).isNotNull();

        assertThat(meterRegistry.get("quotaguard.penalties.applied")
                .tag("type", penaltyType)
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @Order(8)
    void sessionStartAndEndIncrementsCountersAndUpdatesGauge() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/sessions/start")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientReference\":\"desktop-client\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        sessionId = UUID.fromString(objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        Gauge activeSessionsGauge = meterRegistry.find("quotaguard.sessions.active").gauge();
        assertThat(activeSessionsGauge).isNotNull();
        assertThat(activeSessionsGauge.value()).isEqualTo(1.0);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/end", sessionId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountConsumed", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"));

        assertThat(meterRegistry.get("quotaguard.sessions.completed").counter().count()).isEqualTo(1.0);
        assertThat(activeSessionsGauge.value()).isEqualTo(0.0);
        assertThat(meterRegistry.get("quotaguard.timer.session.completion").timer().count()).isEqualTo(1L);
    }

    @Test
    @Order(9)
    void failedSessionCompletionIncrementsCounter() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/end", sessionId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountConsumed", 5))))
                .andExpect(status().isConflict());

        assertThat(meterRegistry.get("quotaguard.sessions.completion.failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @Order(10)
    void adminUserCreateIncrementsAdminOperationsCounter() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
        adminToken = loginAndExtractToken(adminEmail, adminPassword);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "managed-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!",
                                "role", "USER"
                        ))))
                .andExpect(status().isCreated());

        assertThat(meterRegistry.get("quotaguard.admin.operations")
                .tag("type", "user_create")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @Order(11)
    void adminQuotaResetIncrementsBulkResetCounterAndTimer() throws Exception {
        mockMvc.perform(post("/api/v1/quota/reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetCount").isNumber());

        assertThat(meterRegistry.get("quotaguard.quota.resets")
                .tag("type", "bulk")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("quotaguard.admin.operations")
                .tag("type", "quota_reset")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("quotaguard.timer.quota.reset").timer().count()).isEqualTo(1L);
    }

    @Test
    @Order(12)
    void metricsEndpointExposesCustomMetricNames() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("quotaguard.registrations.successful")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.logins.successful")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.quota.consumptions")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.quota.resets")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.penalties.applied")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.sessions.active")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.sessions.completed")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.sessions.completion.failed")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.admin.operations")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.timer.registration")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.timer.login")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.timer.quota.consumption")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.timer.session.completion")))
                .andExpect(jsonPath("$.names", hasItem("quotaguard.timer.quota.reset")));
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("access_token")
                .asText();
    }
}

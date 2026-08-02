package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.audit.domain.AuditEvent;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.penalty.repository.PenaltyEventRepository;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.quota.service.QuotaService;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.repository.UserRepository;
import com.snor.quotaguard.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuotaRepository userQuotaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private PenaltyEventRepository penaltyEventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void adminUserLifecycleProducesAuditEvents() throws Exception {
        User admin = seedAdmin();
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "managed-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!",
                                "role", "USER"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID userId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText());

        List<AuditEvent> events = eventsForResource(userId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAction()).isEqualTo(AuditAction.USER_CREATED);
        assertThat(events.get(0).getActorId()).isEqualTo(admin.getId());
        assertThat(events.get(0).getActorEmail()).isEqualTo(admin.getEmail());
        assertThat(events.get(0).getResourceType()).isEqualTo("USER");
        assertThat(events.get(0).isSuccess()).isTrue();

        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(eventsForResource(userId))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.USER_CREATED, AuditAction.USER_UPDATED, AuditAction.USER_DELETED);
    }

    @Test
    void loginSuccessAndFailureAreAudited() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        register(email, "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk());

        AuditEvent success = latestEvent(AuditAction.LOGIN_SUCCESS, email);
        assertThat(success).isNotNull();
        assertThat(success.getActorEmail()).isEqualTo(email);
        assertThat(success.isSuccess()).isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "WrongPassword!"
                        ))))
                .andExpect(status().isUnauthorized());

        AuditEvent failure = latestEvent(AuditAction.LOGIN_FAILED, email);
        assertThat(failure).isNotNull();
        assertThat(failure.getActorId()).isNull();
        assertThat(failure.getActorEmail()).isEqualTo(email);
        assertThat(failure.isSuccess()).isFalse();
    }

    @Test
    void registrationSuccessAndFailureAreAudited() throws Exception {
        String email = "register-" + UUID.randomUUID() + "@example.com";
        MvcResult result = register(email, "Password123!");
        UUID registeredId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("user").get("id").asText());

        AuditEvent success = latestEvent(AuditAction.REGISTER_SUCCESS, email);
        assertThat(success).isNotNull();
        assertThat(success.getActorId()).isEqualTo(registeredId);
        assertThat(success.isSuccess()).isTrue();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isConflict());

        AuditEvent failure = latestEvent(AuditAction.REGISTER_FAILED, email);
        assertThat(failure).isNotNull();
        assertThat(failure.getActorId()).isNull();
        assertThat(failure.getActorEmail()).isEqualTo(email);
        assertThat(failure.isSuccess()).isFalse();
    }

    @Test
    void adminQuotaResetProducesAuditEvent() throws Exception {
        User admin = seedAdmin();
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(post("/api/v1/quota/reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        AuditEvent reset = latestEvent(AuditAction.QUOTA_RESET, null);
        assertThat(reset).isNotNull();
        assertThat(reset.getActorId()).isEqualTo(admin.getId());
        assertThat(reset.getDescription()).contains("quotas");
    }

    @Test
    void systemResetRecordsAuditWithoutActor() {
        quotaService.resetAllQuotasAndExpirePenalties();

        AuditEvent reset = latestEvent(AuditAction.QUOTA_RESET, null);
        assertThat(reset).isNotNull();
        assertThat(reset.getActorId()).isNull();
        assertThat(reset.getActorEmail()).isNull();
    }

    @Test
    void sessionLifecycleIsAudited() throws Exception {
        String email = "session-audit-" + UUID.randomUUID() + "@example.com";
        MvcResult registerResult = register(email, "Password123!");
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("access_token").asText();

        MvcResult startResult = mockMvc.perform(post("/api/v1/sessions/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountConsumed", 1))))
                .andExpect(status().isOk());

        assertThat(eventsForResource(sessionId))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.SESSION_STARTED, AuditAction.SESSION_COMPLETED);
    }

    @Test
    void penaltyApplicationIsAudited() throws Exception {
        String email = "penalty-audit-" + UUID.randomUUID() + "@example.com";
        MvcResult registerResult = register(email, "Password123!");
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("access_token").asText();
        UUID userId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("user").get("id").asText());

        UserQuota quota = userQuotaRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        quota.setDailyLimit(10);
        quota.setUsedToday(10);
        userQuotaRepository.save(quota);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/usage/consume")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "amountConsumed", 5,
                                    "actionType", "API_CALL"
                            ))))
                    .andExpect(status().isTooManyRequests());
        }

        List<AuditEvent> penalties = auditEventRepository.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.PENALTY_APPLIED)
                .toList();
        assertThat(penalties).hasSize(2);
        assertThat(penalties).allSatisfy(event -> {
            assertThat(event.getActorId()).isEqualTo(userId);
            assertThat(event.getResourceType()).isEqualTo("PENALTY");
            assertThat(event.getResourceId()).isNotNull();
        });
    }

    @Test
    void penaltyExpiryIsAudited() throws Exception {
        String email = "expiry-audit-" + UUID.randomUUID() + "@example.com";
        MvcResult registerResult = register(email, "Password123!");
        UUID userId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("user").get("id").asText());

        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        penaltyEventRepository.save(PenaltyEvent.builder()
                .user(user)
                .type(PenaltyType.SHORT_COOLDOWN)
                .startTime(now.minusHours(1))
                .endTime(now.minusMinutes(1))
                .active(true)
                .build());

        quotaService.resetAllQuotasAndExpirePenalties();

        AuditEvent expired = latestEvent(AuditAction.PENALTY_EXPIRED, null);
        assertThat(expired).isNotNull();
        assertThat(expired.getResourceType()).isEqualTo("PENALTY");
        assertThat(expired.getActorId()).isNull();
        assertThat(expired.getActorEmail()).isNull();
        assertThat(expired.isSuccess()).isTrue();
    }

    @Test
    void rolledBackOperationLeavesNoSuccessAuditEvent() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        UUID[] createdIds = new UUID[1];
        transactionTemplate.execute(status -> {
            var response = userService.createUser(new CreateUserRequest(
                    "rollback-" + UUID.randomUUID() + "@example.com",
                    "Password123!",
                    null
            ));
            createdIds[0] = response.id();
            status.setRollbackOnly();
            return null;
        });

        assertThat(eventsForResource(createdIds[0])).isEmpty();
    }

    @Test
    void auditReadApiIsAdminOnlyAndPaged() throws Exception {
        User admin = seedAdmin();
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        MvcResult listResult = mockMvc.perform(get("/api/v1/audit")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "timestamp,desc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();
        JsonNode page = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(page.get("content")).isNotEmpty();

        String eventId = page.get("content").get(0).get("id").asText();
        mockMvc.perform(get("/api/v1/audit/{eventId}", eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.action").exists())
                .andExpect(jsonPath("$.success").exists());

        mockMvc.perform(get("/api/v1/audit/{eventId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/audit")
                        .param("sort", "unknownProperty,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regularUserCannotReadAuditTrail() throws Exception {
        String email = "user-audit-" + UUID.randomUUID() + "@example.com";
        MvcResult registerResult = register(email, "Password123!");
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("access_token").asText();

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private User seedAdmin() {
        return userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private MvcResult register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
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

    private List<AuditEvent> eventsForResource(UUID resourceId) {
        return auditEventRepository.findAll().stream()
                .filter(event -> resourceId.equals(event.getResourceId()))
                .sorted(Comparator.comparing(AuditEvent::getTimestamp))
                .toList();
    }

    private AuditEvent latestEvent(AuditAction action, String actorEmail) {
        return auditEventRepository.findAll().stream()
                .filter(event -> event.getAction() == action)
                .filter(event -> actorEmail == null || actorEmail.equals(event.getActorEmail()))
                .max(Comparator.comparing(AuditEvent::getTimestamp))
                .orElse(null);
    }
}

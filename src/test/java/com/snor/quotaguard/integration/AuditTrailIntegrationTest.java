package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.audit.repository.AuditEventRepository;
import com.snor.quotaguard.domain.AuditEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.AuditAction;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.quota.service.QuotaService;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private QuotaService quotaService;

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
        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        AuditEvent createEvent = findEventForResource(userId);
        assertThat(createEvent).isNotNull();
        assertThat(createEvent.getActorId()).isEqualTo(admin.getId());
        assertThat(createEvent.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(createEvent.getResource()).isEqualTo("USER");
        assertThat(createEvent.getDetails()).containsEntry("role", "USER");

        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk());

        AuditEvent updateEvent = findEventForResource(userId);
        assertThat(updateEvent).isNotNull();
        assertThat(updateEvent.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(updateEvent.getActorId()).isEqualTo(admin.getId());
        assertThat(updateEvent.getDetails()).containsEntry("roleChanged", "true");

        mockMvc.perform(delete("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        List<AuditEvent> events = eventsForResource(userId);
        assertThat(events).extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
    }

    @Test
    void adminQuotaResetProducesAuditEvent() throws Exception {
        User admin = seedAdmin();
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(post("/api/v1/quota/reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<AuditEvent> resets = auditEventRepository.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.RESET && "QUOTA".equals(event.getResource()))
                .toList();
        assertThat(resets).isNotEmpty();
        AuditEvent resetEvent = resets.get(resets.size() - 1);
        assertThat(resetEvent.getActorId()).isEqualTo(admin.getId());
        assertThat(resetEvent.getResourceId()).isNull();
        assertThat(resetEvent.getDetails()).containsKeys("resetCount", "expiredPenalties");
    }

    @Test
    void publicRegistrationIsNotAudited() throws Exception {
        String email = "self-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated());

        User registered = userRepository.findByEmail(email).orElseThrow();
        assertThat(findEventForResource(registered.getId().toString())).isNull();
    }

    @Test
    void systemResetRecordsAuditWithoutActor() {
        quotaService.resetAllQuotasAndExpirePenalties();

        List<AuditEvent> resets = auditEventRepository.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.RESET && "QUOTA".equals(event.getResource()))
                .toList();
        assertThat(resets).isNotEmpty();
        assertThat(resets.get(resets.size() - 1).getActorId()).isNull();
    }

    private User seedAdmin() {
        return userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
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

    private AuditEvent findEventForResource(String resourceId) {
        List<AuditEvent> events = eventsForResource(resourceId);
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }

    private List<AuditEvent> eventsForResource(String resourceId) {
        return auditEventRepository.findAll().stream()
                .filter(event -> resourceId.equals(event.getResourceId()))
                .toList();
    }
}

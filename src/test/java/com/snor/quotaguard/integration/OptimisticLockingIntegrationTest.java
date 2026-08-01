package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.UsageSession;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.domain.enums.SessionStatus;
import com.snor.quotaguard.penalty.repository.PenaltyEventRepository;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.session.repository.UsageSessionRepository;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OptimisticLockingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuotaRepository userQuotaRepository;

    @Autowired
    private UsageSessionRepository usageSessionRepository;

    @Autowired
    private PenaltyEventRepository penaltyEventRepository;

    @Test
    void quotaVersionIncrementsAndStaleUpdateConflicts() throws Exception {
        User user = registerUser();
        UserQuota fresh = userQuotaRepository.findByUser(user).orElseThrow();
        UserQuota stale = userQuotaRepository.findByUser(user).orElseThrow();

        fresh.setUsedToday(10);
        userQuotaRepository.saveAndFlush(fresh);
        assertThat(userQuotaRepository.findByUser(user).orElseThrow().getVersion()).isEqualTo(1);

        stale.setUsedToday(20);
        assertThatThrownBy(() -> userQuotaRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void sessionVersionIncrementsAndStaleUpdateConflicts() throws Exception {
        User user = registerUser();
        UsageSession session = usageSessionRepository.save(UsageSession.builder()
                .user(user)
                .clientReference("concurrency-test")
                .startedAt(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build());

        UsageSession fresh = usageSessionRepository.findById(session.getId()).orElseThrow();
        UsageSession stale = usageSessionRepository.findById(session.getId()).orElseThrow();

        fresh.setMetadata("updated-by-fresh-writer");
        usageSessionRepository.saveAndFlush(fresh);
        assertThat(usageSessionRepository.findById(session.getId()).orElseThrow().getVersion()).isEqualTo(1);

        stale.setMetadata("updated-by-stale-writer");
        assertThatThrownBy(() -> usageSessionRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void penaltyEventVersionIncrementsAndStaleUpdateConflicts() throws Exception {
        User user = registerUser();
        LocalDateTime now = LocalDateTime.now();
        PenaltyEvent event = penaltyEventRepository.save(PenaltyEvent.builder()
                .user(user)
                .type(PenaltyType.WARNING)
                .startTime(now)
                .endTime(now)
                .active(false)
                .build());

        PenaltyEvent fresh = penaltyEventRepository.findById(event.getId()).orElseThrow();
        PenaltyEvent stale = penaltyEventRepository.findById(event.getId()).orElseThrow();

        fresh.setActive(true);
        penaltyEventRepository.saveAndFlush(fresh);
        assertThat(penaltyEventRepository.findById(event.getId()).orElseThrow().getVersion()).isEqualTo(1);

        stale.setActive(true);
        assertThatThrownBy(() -> penaltyEventRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private User registerUser() throws Exception {
        String email = "lock-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registered = objectMapper.readTree(response);
        return userRepository.findById(UUID.fromString(registered.get("user").get("id").asText()))
                .orElseThrow();
    }
}

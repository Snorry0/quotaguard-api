package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsageSessionApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuotaRepository userQuotaRepository;

    @Test
    void failedSessionEndKeepsSessionActiveAndRetryable() throws Exception {
        MvcResult registerResult = registerUser();
        JsonNode registered = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registered.get("access_token").asText();
        String userId = registered.get("user").get("id").asText();

        shrinkQuotaToLimit(userId, 10, 10);

        MvcResult startResult = mockMvc.perform(post("/api/v1/sessions/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountConsumed", 20))))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/v1/sessions/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endedAt").value(nullValue()));

        shrinkQuotaToLimit(userId, 10, 0);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountConsumed", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.consumption.usage.amountConsumed").value(5));
    }

    @Test
    void startSessionIsBlockedByActiveCooldownPenalty() throws Exception {
        MvcResult registerResult = registerUser();
        JsonNode registered = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registered.get("access_token").asText();
        String userId = registered.get("user").get("id").asText();

        shrinkQuotaToLimit(userId, 10, 10);

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

        mockMvc.perform(post("/api/v1/sessions/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());
    }

    private MvcResult registerUser() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "session-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private void shrinkQuotaToLimit(String userId, int dailyLimit, int usedToday) throws Exception {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        UserQuota quota = userQuotaRepository.findByUser(user).orElseThrow();
        quota.setDailyLimit(dailyLimit);
        quota.setUsedToday(usedToday);
        userQuotaRepository.save(quota);
    }
}

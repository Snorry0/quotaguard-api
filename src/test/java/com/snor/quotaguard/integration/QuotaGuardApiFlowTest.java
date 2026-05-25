package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuotaGuardApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanRegisterLoginReadQuotaAndConsumeUsage() throws Exception {
        String email = "api-flow-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        registerUser(email, password);

        String accessToken = loginAndExtractToken(email, password);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(get("/api/v1/quota")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").exists())
                .andExpect(jsonPath("$.usedToday").exists())
                .andExpect(jsonPath("$.remainingToday").exists())
                .andExpect(jsonPath("$.penaltyLevel").exists());

        mockMvc.perform(post("/api/v1/usage/consume")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountConsumed", 10,
                                "actionType", "API_CALL"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usage.amountConsumed").value(10))
                .andExpect(jsonPath("$.usage.actionType").value("API_CALL"))
                .andExpect(jsonPath("$.quota.usedToday").value(10))
                .andExpect(jsonPath("$.quota.remainingToday").exists());

        mockMvc.perform(get("/api/v1/usage/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    private void registerUser(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        return response.get("access_token").asText();
    }
}
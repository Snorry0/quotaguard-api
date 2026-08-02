package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the validation layer — validation-layer spec, section 3.3.
 * Proves 400 for malformed input, 200/201 for valid input, and the
 * {@code field/rejectedValue/message} error contract (including the
 * {@code ConstraintViolationException}/{@code HandlerMethodValidationException}
 * paths that used to surface as 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "weak-" + UUID.randomUUID() + "@example.com",
                                "password", "password"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void registerRejectsWhitespaceEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "  a@b.com  ",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem("  a@b.com  ")))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void registerRejectsUppercaseEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "A@B.COM",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem("A@B.COM")))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void registerAcceptsValidEmailAndStrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "valid-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.email").isNotEmpty());
    }

    @Test
    void historyRejectsNegativePageWith400InsteadOf500() throws Exception {
        String token = accessToken(register("history-neg-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(get("/api/v1/usage/history")
                        .param("page", "-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.page").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("page")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem(-1)))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void adminUserListRejectsOversizedPageSize() throws Exception {
        String adminToken = seedAdminAndLogin();

        mockMvc.perform(get("/api/v1/users")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.size").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem(101)))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void usageStatsRejectsDaysAboveMax() throws Exception {
        String token = accessToken(register("days-max-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(get("/api/v1/stats/usage")
                        .param("days", "400")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.days").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("days")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem(400)))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void usageStatsRejectsZeroDays() throws Exception {
        String token = accessToken(register("days-zero-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(get("/api/v1/stats/usage")
                        .param("days", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.days").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("days")))
                .andExpect(jsonPath("$.errors[*].rejectedValue", hasItem(0)))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void historyAcceptsValidPagination() throws Exception {
        String token = accessToken(register("history-ok-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(get("/api/v1/usage/history")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void patchRejectsBlankEmail() throws Exception {
        String adminToken = seedAdminAndLogin();
        String targetUserId = userId(register("patch-blank-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(patch("/api/v1/users/{id}", targetUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].message").isNotEmpty());
    }

    @Test
    void patchOmittingEmailIsAccepted() throws Exception {
        String adminToken = seedAdminAndLogin();
        String targetUserId = userId(register("patch-omit-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(patch("/api/v1/users/{id}", targetUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "NewPass123!"))))
                .andExpect(status().isOk());
    }

    @Test
    void patchWithValidNormalizedEmailIsAccepted() throws Exception {
        String adminToken = seedAdminAndLogin();
        String targetUserId = userId(register("patch-valid-" + UUID.randomUUID() + "@example.com"));

        mockMvc.perform(patch("/api/v1/users/{id}", targetUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "new-" + UUID.randomUUID() + "@example.com"))))
                .andExpect(status().isOk());
    }

    private String seedAdminAndLogin() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
        return loginAndExtractToken(adminEmail, adminPassword);
    }

    private MvcResult register(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String accessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("access_token").asText();
    }

    private String userId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("user").get("id").asText();
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

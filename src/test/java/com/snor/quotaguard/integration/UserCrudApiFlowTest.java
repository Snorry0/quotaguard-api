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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserCrudApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanCreateReadUpdateAndDeleteUsers() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        User admin = userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String accessToken = loginAndExtractToken(adminEmail, adminPassword);

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "managed-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!",
                                "role", "USER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        JsonNode createdUser = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = createdUser.get("id").asText();

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));

        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "updated-" + UUID.randomUUID() + "@example.com",
                                "role", "USER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(delete("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        userRepository.deleteById(admin.getId());
    }

    @Test
    void creatingUserWithDuplicateEmailReturnsConflict() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String accessToken = loginAndExtractToken(adminEmail, adminPassword);

        String existingEmail = "existing-" + UUID.randomUUID() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "email", existingEmail,
                "password", "Password123!",
                "role", "USER"
        ));

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account already exists for email: " + existingEmail));
    }

    @Test
    void creatingUserWithInvalidRoleReturnsBadRequest() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String accessToken = loginAndExtractToken(adminEmail, adminPassword);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!",
                                "role", "SUPERUSER"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotDeleteOwnAccount() throws Exception {
        String adminEmail = "self-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        User admin = userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String accessToken = loginAndExtractToken(adminEmail, adminPassword);

        mockMvc.perform(delete("/api/v1/users/{userId}", admin.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Admins cannot delete their own account"));
    }

    @Test
    void tokenRemainsValidAfterEmailChange() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
        String adminToken = loginAndExtractToken(adminEmail, adminPassword);

        String userEmail = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userEmail,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode registered = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String userId = registered.get("user").get("id").asText();
        String oldToken = registered.get("access_token").asText();

        String newEmail = "renamed-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", newEmail))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    void adminCanResetAllQuotas() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String adminPassword = "Password123!";
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String accessToken = loginAndExtractToken(adminEmail, adminPassword);

        mockMvc.perform(post("/api/v1/quota/reset")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetCount").isNumber())
                .andExpect(jsonPath("$.resetDate").exists())
                .andExpect(jsonPath("$.expiredPenalties").isNumber());
    }

    @Test
    void regularUsersCannotAccessUserAdministration() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isCreated());

        String accessToken = loginAndExtractToken(email, password);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
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

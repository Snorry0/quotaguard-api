package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.audit.AuditCommand;
import com.snor.quotaguard.audit.service.AuditEventWriter;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaguard-isolation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class AuditIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuditEventWriter auditEventWriter;

    @Test
    void businessOperationSucceedsWhenAuditPersistenceFails() throws Exception {
        doThrow(new RuntimeException("audit database is down"))
                .when(auditEventWriter).persist(any(AuditCommand.class));

        User admin = userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", admin.getEmail(),
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("access_token").asText();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "survives-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/quota/reset")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}

package com.snor.quotaguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.user.repository.UserRepository;
import com.snor.quotaguard.user.service.UserService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration proof of the application cache: hit / miss / eviction for the
 * {@code users} + {@code adminQueries} caches against the real {@code UserService}
 * annotations. The {@code UserRepository} is a {@code @SpyBean} so the DB
 * invocation count is verifiable — the second read of a cached key must NOT
 * reach the repository.
 *
 * <p>Uses a dedicated in-memory database (mirroring {@code RateLimitIntegrationTest})
 * so this class gets its own Spring context + a pristine cache. The ordered
 * tests share the seeded user state ({@code PER_CLASS}); the cache-key asserts
 * use the exact cache keys the service SpEL produces (the normalized email /
 * {@code "page-size"}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaguard-cache;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    /**
     * The user seeded by {@code cacheMissThenHitForEmailLookup} (normalized email).
     * NOTE: the spy invocation counts are reset before EVERY test method
     * (Spring Boot's {@code MockitoTestExecutionListener}) — each test verifies
     * only its own interactions; the Caffeine cache state persists across the
     * ordered methods.
     */
    private String email;

    /** The user seeded by {@code cacheEvictsOnUpdate} (its original email + id). */
    private String evictEmail;

    /** The evict user's email after {@code cacheEvictsOnUpdate} renamed it. */
    private String updatedEmail;

    private UUID userId;

    private UUID evictUserId;

    private String adminToken;

    @Test
    @Order(1)
    void cacheMissThenHitForEmailLookup() {
        email = uniqueEmail("cache-miss-hit");
        User user = seedUser(email, Role.USER);
        userId = user.getId();

        Cache users = cacheManager.getCache("users");
        assertThat(users).isNotNull();
        assertThat(users.get(email)).isNull(); // MISS: nothing cached before the first read

        userService.findUserByEmail(email); // miss -> DB -> cached
        User second = userService.findUserByEmail(email); // HIT: served from the cache

        verify(userRepository, times(1)).findByEmail(email); // the second read did NOT hit the DB
        assertThat(second.getId()).isEqualTo(userId);
        assertThat(users.get(email)).isNotNull(); // the entry is now cached
    }

    @Test
    @Order(2)
    void cacheKeyedByNormalizedEmail() {
        // The SpEL key normalizes the input, so an UN-normalized lookup
        // ("Demo@Example.COM") hits the SAME entry @Order(1) cached under the
        // normalized email — the cache state persists across the ordered tests,
        // while the spy counts were reset. Zero repository interaction proves
        // the entry was served entirely from the cache.
        User hit = userService.findUserByEmail(email.toUpperCase());

        assertThat(hit.getId()).isEqualTo(userId);
        verify(userRepository, never()).findByEmail(anyString());
        assertThat(cacheManager.getCache("users").get(email)).isNotNull();
    }

    @Test
    @Order(3)
    void cacheEvictsOnUpdate() throws Exception {
        evictEmail = uniqueEmail("cache-evict");
        User evictUser = seedUser(evictEmail, Role.USER);
        evictUserId = evictUser.getId();

        String adminEmail = uniqueEmail("cache-admin");
        seedUser(adminEmail, Role.ADMIN);
        adminToken = loginAndExtractToken(adminEmail, PASSWORD);

        // Cache the user under its email so the eviction has a real entry to clear.
        userService.findUserByEmail(evictEmail);
        Cache users = cacheManager.getCache("users");
        assertThat(users).isNotNull();
        assertThat(users.get(evictEmail)).isNotNull();

        // Populate the admin list cache, then rename the user via the admin PATCH.
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        Cache adminQueries = cacheManager.getCache("adminQueries");
        assertThat(adminQueries).isNotNull();
        assertThat(adminQueries.get("0-20")).isNotNull();

        updatedEmail = uniqueEmail("cache-updated");
        mockMvc.perform(patch("/api/v1/users/{userId}", evictUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", updatedEmail))))
                .andExpect(status().isOk());

        // The allEntries eviction clears the users cache (old- AND new-email keys)
        // plus the admin list cache.
        assertThat(users.get(evictEmail)).isNull();
        assertThat(users.get(updatedEmail)).isNull();
        assertThat(adminQueries.get("0-20")).isNull();

        // The next read is a miss again: the repository serves it.
        User updated = userService.findUserByEmail(updatedEmail);
        assertThat(updated.getEmail()).isEqualTo(updatedEmail);
        verify(userRepository, times(1)).findByEmail(updatedEmail);
    }

    @Test
    @Order(4)
    void cacheEvictsOnDelete() throws Exception {
        // The entry @Order(3) cached under the updated email must still be present.
        Cache users = cacheManager.getCache("users");
        assertThat(users).isNotNull();
        assertThat(users.get(updatedEmail)).isNotNull();

        // Repopulate the admin list cache, then delete the user via the admin DELETE.
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        Cache adminQueries = cacheManager.getCache("adminQueries");
        assertThat(adminQueries).isNotNull();
        assertThat(adminQueries.get("0-20")).isNotNull();

        mockMvc.perform(delete("/api/v1/users/{userId}", evictUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // The delete evicted the users cache: the entry is gone and the lookup
        // misses BOTH the cache and the DB.
        assertThat(users.get(updatedEmail)).isNull();

        assertThatThrownBy(() -> userService.findUserByEmail(updatedEmail))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, times(1)).findByEmail(updatedEmail); // this test's miss only
        assertThat(adminQueries.get("0-20")).isNull(); // the admin list cache evicted too
    }

    private User seedUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
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

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}

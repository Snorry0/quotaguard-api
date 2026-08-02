package com.snor.quotaguard.user.service;

import com.snor.quotaguard.audit.service.AuditEventService;
import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.exception.EmailAlreadyExistsException;
import com.snor.quotaguard.exception.SelfDeletionNotAllowedException;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.dto.request.UpdateUserRequest;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserQuotaRepository userQuotaRepository = mock(UserQuotaRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
    private final QuotaGuardProperties properties = new QuotaGuardProperties(
            100,
            1,
            new QuotaGuardProperties.Penalties(Duration.ofMinutes(15), Duration.ofHours(4)),
            new QuotaGuardProperties.Sessions(1, 1)
    );

    private final UserService userService = new UserService(
            userRepository,
            userQuotaRepository,
            userMapper,
            currentUserProvider,
            passwordEncoder,
            properties,
            auditEventService,
            clock
    );

    @Test
    void createUserNormalizesEmailDefaultsRoleAndCreatesQuota() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        userService.createUserEntity(new CreateUserRequest(" Test@Example.COM ", "Password123!", null));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded");

        ArgumentCaptor<UserQuota> quotaCaptor = ArgumentCaptor.forClass(UserQuota.class);
        verify(userQuotaRepository).save(quotaCaptor.capture());
        assertThat(quotaCaptor.getValue().getDailyLimit()).isEqualTo(100);
        assertThat(quotaCaptor.getValue().getUsedToday()).isZero();
        assertThat(quotaCaptor.getValue().getPenaltyLevel()).isZero();
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUserEntity(
                new CreateUserRequest("taken@example.com", "Password123!", null)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userQuotaRepository, never()).save(any(UserQuota.class));
    }

    @Test
    void createUserTranslatesUniqueViolationToConflict() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));
        when(userRepository.existsByEmail("race@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUserEntity(
                new CreateUserRequest("race@example.com", "Password123!", Role.USER)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void updateUserKeepsExistingPasswordAndEmailWhenNotProvided() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("same@example.com")
                .passwordHash("existing-hash")
                .role(Role.USER)
                .createdAt(LocalDateTime.now(clock))
                .build();
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(existing.getId(), new UpdateUserRequest(null, null, Role.ADMIN));

        verify(passwordEncoder, never()).encode(anyString());
        assertThat(existing.getEmail()).isEqualTo("same@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateUserRejectsEmailAlreadyUsedByAnotherUser() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("current@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("other@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(
                existing.getId(),
                new UpdateUserRequest("other@example.com", null, null)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void updateUserEncodesNewPasswordWhenProvided() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("same@example.com")
                .passwordHash("old-hash")
                .role(Role.USER)
                .build();
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(
                existing.getId(),
                new UpdateUserRequest("same@example.com", "NewPassword123!", null)
        );

        verify(passwordEncoder).encode("NewPassword123!");
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void deleteUserRemovesQuotaBeforeUser() {
        User actor = User.builder()
                .id(UUID.randomUUID())
                .email("actor@example.com")
                .role(Role.ADMIN)
                .build();
        when(currentUserProvider.getCurrentUser()).thenReturn(actor);
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("delete@example.com")
                .role(Role.USER)
                .build();
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        UserQuota quota = UserQuota.builder().id(UUID.randomUUID()).user(existing).build();
        when(userQuotaRepository.findByUser(existing)).thenReturn(Optional.of(quota));

        userService.deleteUser(existing.getId());

        InOrder inOrder = inOrder(userQuotaRepository, userRepository);
        inOrder.verify(userQuotaRepository).delete(quota);
        inOrder.verify(userRepository).delete(existing);
    }

    @Test
    void deleteUserRejectsSelfDeletion() {
        User actor = User.builder()
                .id(UUID.randomUUID())
                .email("self@example.com")
                .role(Role.ADMIN)
                .build();
        when(currentUserProvider.getCurrentUser()).thenReturn(actor);

        assertThatThrownBy(() -> userService.deleteUser(actor.getId()))
                .isInstanceOf(SelfDeletionNotAllowedException.class);

        verify(userRepository, never()).delete(any(User.class));
        verify(userQuotaRepository, never()).delete(any(UserQuota.class));
    }
}

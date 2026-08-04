package com.snor.quotaguard.user.service;

import com.snor.quotaguard.common.PageRequestFactory;
import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.UserCreatedEvent;
import com.snor.quotaguard.event.UserDeletedEvent;
import com.snor.quotaguard.event.UserUpdatedEvent;
import com.snor.quotaguard.exception.EmailAlreadyExistsException;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.exception.SelfDeletionNotAllowedException;
import com.snor.quotaguard.quota.repository.UserQuotaRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import com.snor.quotaguard.user.EmailNormalizer;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.dto.request.UpdateUserRequest;
import com.snor.quotaguard.user.dto.response.UserResponse;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserQuotaRepository userQuotaRepository;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final QuotaGuardProperties properties;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return userMapper.toResponse(currentUserProvider.getCurrentUser());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminQueries", key = "#page + '-' + #size")
    public Page<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequestFactory.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "email")
        );
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "#userId")
    public UserResponse getUser(UUID userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    @CacheEvict(cacheNames = {"users", "adminQueries"}, allEntries = true, beforeInvocation = true)
    public UserResponse createUser(CreateUserRequest request) {
        User savedUser = createUserEntity(request);
        domainEventPublisher.publish(new UserCreatedEvent(
                Instant.now(clock),
                Actor.of(currentUserProvider.getCurrentUserIfPresent()),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        ));
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @CacheEvict(cacheNames = {"users", "adminQueries"}, allEntries = true, beforeInvocation = true)
    public User createUserEntity(CreateUserRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        ensureEmailAvailable(normalizedEmail);

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role() == null ? Role.USER : request.role())
                .createdAt(LocalDateTime.now(clock))
                .build();
        User savedUser = saveUser(user);
        userQuotaRepository.save(UserQuota.builder()
                .user(savedUser)
                .dailyLimit(properties.defaultDailyLimit())
                .usedToday(0)
                .lastResetDate(LocalDate.now(clock))
                .penaltyLevel(0)
                .build());
        log.info("User created id={} role={}", savedUser.getId(), savedUser.getRole());
        return savedUser;
    }

    @Transactional
    @CacheEvict(cacheNames = {"users", "adminQueries"}, allEntries = true, beforeInvocation = true)
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUser(userId);
        Map<String, String> changes = new LinkedHashMap<>();
        if (request.email() != null) {
            String normalizedEmail = EmailNormalizer.normalize(request.email());
            if (!normalizedEmail.equals(user.getEmail())) {
                ensureEmailAvailable(normalizedEmail);
                user.setEmail(normalizedEmail);
                changes.put("emailChanged", "true");
            }
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            changes.put("passwordChanged", "true");
        }
        if (request.role() != null) {
            user.setRole(request.role());
            changes.put("roleChanged", "true");
        }
        User updatedUser = saveUser(user);
        log.info("User updated id={} role={}", updatedUser.getId(), updatedUser.getRole());
        if (!changes.isEmpty()) {
            domainEventPublisher.publish(new UserUpdatedEvent(
                    Instant.now(clock),
                    Actor.of(currentUserProvider.getCurrentUserIfPresent()),
                    userId,
                    changes.keySet()
            ));
        }
        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    @CacheEvict(cacheNames = {"users", "adminQueries"}, allEntries = true, beforeInvocation = true)
    public void deleteUser(UUID userId) {
        User currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new SelfDeletionNotAllowedException();
        }
        User user = findUser(userId);
        userQuotaRepository.findByUser(user).ifPresent(userQuotaRepository::delete);
        userRepository.delete(user);
        log.info("User deleted id={}", userId);
        domainEventPublisher.publish(new UserDeletedEvent(
                Instant.now(clock),
                Actor.of(currentUser),
                userId
        ));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "T(com.snor.quotaguard.user.EmailNormalizer).normalize(#email)", sync = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User saveUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new EmailAlreadyExistsException(user.getEmail());
            }
            throw ex;
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }
}

package com.snor.quotaguard.user.service;

import com.snor.quotaguard.common.PageRequestFactory;
import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.Role;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return userMapper.toResponse(currentUserProvider.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequestFactory.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "email")
        );
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return userMapper.toResponse(createUserEntity(request));
    }

    @Transactional
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
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUser(userId);
        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = EmailNormalizer.normalize(request.email());
            if (!normalizedEmail.equals(user.getEmail())) {
                ensureEmailAvailable(normalizedEmail);
                user.setEmail(normalizedEmail);
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        User updatedUser = saveUser(user);
        log.info("User updated id={} role={}", updatedUser.getId(), updatedUser.getRole());
        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new SelfDeletionNotAllowedException();
        }
        User user = findUser(userId);
        userQuotaRepository.findByUser(user).ifPresent(userQuotaRepository::delete);
        userRepository.delete(user);
        log.info("User deleted id={}", userId);
    }

    @Transactional(readOnly = true)
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

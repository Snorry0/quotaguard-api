package com.snor.quotaguard.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.dto.request.LoginRequest;
import com.snor.quotaguard.dto.request.RegisterRequest;
import com.snor.quotaguard.dto.response.AuthResponse;
import com.snor.quotaguard.exception.EmailAlreadyExistsException;
import com.snor.quotaguard.mapper.UserMapper;
import com.snor.quotaguard.repository.UserQuotaRepository;
import com.snor.quotaguard.repository.UserRepository;
import com.snor.quotaguard.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserQuotaRepository userQuotaRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final QuotaGuardProperties properties;
    private final Clock clock;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now(clock))
                .build();

        User savedUser = userRepository.save(user);
        userQuotaRepository.save(UserQuota.builder()
                .user(savedUser)
                .dailyLimit(properties.defaultDailyLimit())
                .usedToday(0)
                .lastResetDate(LocalDate.now(clock))
                .penaltyLevel(0)
                .build());

        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token, jwtService.getExpirationInstant(), userMapper.toResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getExpirationInstant(), userMapper.toResponse(user));
    }
}

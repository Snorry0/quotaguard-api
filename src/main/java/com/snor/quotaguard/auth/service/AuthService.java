package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.audit.AuditPublisher;
import com.snor.quotaguard.audit.domain.AuditAction;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.auth.dto.response.AuthResponse;
import com.snor.quotaguard.exception.EmailAlreadyExistsException;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.EmailNormalizer;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RESOURCE_TYPE_AUTH = "AUTH";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserService userService;
    private final AuditPublisher auditPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            User user = userService.createUserEntity(
                    new CreateUserRequest(request.email(), request.password(), Role.USER)
            );
            auditPublisher.publishWithActor(
                    AuditAction.REGISTER_SUCCESS,
                    RESOURCE_TYPE_AUTH,
                    user.getId(),
                    "User registered",
                    true,
                    user.getId(),
                    user.getEmail()
            );
            return new AuthResponse(
                    jwtService.generateToken(user),
                    jwtService.getExpirationInstant(),
                    userMapper.toResponse(user)
            );
        } catch (EmailAlreadyExistsException | DataIntegrityViolationException ex) {
            auditPublisher.publishWithActor(
                    AuditAction.REGISTER_FAILED,
                    RESOURCE_TYPE_AUTH,
                    null,
                    "Registration failed",
                    false,
                    null,
                    EmailNormalizer.normalize(request.email())
            );
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            User user;
            try {
                user = userService.findUserByEmail(normalizedEmail);
            } catch (ResourceNotFoundException ex) {
                throw new BadCredentialsException("Invalid email or password");
            }
            auditPublisher.publishWithActor(
                    AuditAction.LOGIN_SUCCESS,
                    RESOURCE_TYPE_AUTH,
                    user.getId(),
                    "User logged in",
                    true,
                    user.getId(),
                    user.getEmail()
            );
            String token = jwtService.generateToken(user);
            return new AuthResponse(token, jwtService.getExpirationInstant(), userMapper.toResponse(user));
        } catch (AuthenticationException ex) {
            auditPublisher.publishWithActor(
                    AuditAction.LOGIN_FAILED,
                    RESOURCE_TYPE_AUTH,
                    null,
                    "Login failed: invalid credentials",
                    false,
                    null,
                    normalizedEmail
            );
            throw ex;
        }
    }
}

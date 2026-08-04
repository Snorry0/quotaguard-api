package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.auth.dto.response.AuthResponse;
import com.snor.quotaguard.event.Actor;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.LoginFailedEvent;
import com.snor.quotaguard.event.LoginSucceededEvent;
import com.snor.quotaguard.event.RegisterFailedEvent;
import com.snor.quotaguard.event.UserRegisteredEvent;
import com.snor.quotaguard.exception.EmailAlreadyExistsException;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.EmailNormalizer;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.dto.response.UserResponse;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.service.UserService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserService userService;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final RefreshTokenService refreshTokenService;

    @Timed(value = "quotaguard.timer.registration", percentiles = {0.5, 0.95, 0.99})
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            User user = userService.createUserEntity(
                    new CreateUserRequest(request.email(), request.password(), Role.USER)
            );
            domainEventPublisher.publish(new UserRegisteredEvent(
                    Instant.now(clock),
                    Actor.of(user),
                    user.getId(),
                    user.getEmail()
            ));
            String accessToken = jwtService.generateToken(user);
            String refreshToken = refreshTokenService.issue(user).rawToken();
            return new AuthResponse(
                    accessToken,
                    jwtService.getExpirationInstant(),
                    userMapper.toResponse(user),
                    refreshToken
            );
        } catch (EmailAlreadyExistsException | DataIntegrityViolationException ex) {
            domainEventPublisher.publish(new RegisterFailedEvent(
                    Instant.now(clock),
                    EmailNormalizer.normalize(request.email())
            ));
            throw ex;
        }
    }

    @Timed(value = "quotaguard.timer.login", percentiles = {0.5, 0.95, 0.99})
    @Transactional
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
            domainEventPublisher.publish(new LoginSucceededEvent(
                    Instant.now(clock),
                    Actor.of(user),
                    user.getId(),
                    user.getEmail()
            ));
            String accessToken = jwtService.generateToken(user);
            String refreshToken = refreshTokenService.issue(user).rawToken();
            return new AuthResponse(
                    accessToken,
                    jwtService.getExpirationInstant(),
                    userMapper.toResponse(user),
                    refreshToken
            );
        } catch (AuthenticationException ex) {
            domainEventPublisher.publish(new LoginFailedEvent(
                    Instant.now(clock),
                    normalizedEmail
            ));
            throw ex;
        }
    }

    @Timed(value = "quotaguard.timer.refresh", percentiles = {0.5, 0.95, 0.99})
    public AuthResponse refresh(String presentedRefreshToken) {
        RefreshTokenService.RefreshedSession session = refreshTokenService.rotate(presentedRefreshToken);
        User user = session.user();
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthResponse(session.accessToken(), session.expiresAt(), userResponse, session.newRefreshToken());
    }

    public void logout(String presentedRefreshToken) {
        refreshTokenService.revoke(presentedRefreshToken);
    }
}

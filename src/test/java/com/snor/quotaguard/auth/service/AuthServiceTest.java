package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.event.DomainEventPublisher;
import com.snor.quotaguard.event.LoginFailedEvent;
import com.snor.quotaguard.event.LoginSucceededEvent;
import com.snor.quotaguard.event.UserRegisteredEvent;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC);

    private AuthService newAuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            UserService userService,
            DomainEventPublisher domainEventPublisher
    ) {
        return new AuthService(
                authenticationManager, jwtService, userMapper, userService, domainEventPublisher, clock
        );
    }

    @Test
    void registerCreatesUserOnceAndPublishesRegistrationEvent() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        AuthService authService = newAuthService(
                authenticationManager, jwtService, userMapper, userService, domainEventPublisher
        );

        User created = User.builder()
                .id(UUID.randomUUID())
                .email("new@example.com")
                .role(Role.USER)
                .build();
        when(userService.createUserEntity(any())).thenReturn(created);
        when(jwtService.generateToken(created)).thenReturn("token");

        authService.register(new RegisterRequest("new@example.com", "Password123!"));

        verify(userService, times(1)).createUserEntity(any());
        verify(userService, never()).findUserByEmail(anyString());
        verify(jwtService).generateToken(created);

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(created.getId());
        assertThat(captor.getValue().email()).isEqualTo("new@example.com");
    }

    @Test
    void loginPublishesSucceededEvent() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        AuthService authService = newAuthService(
                authenticationManager, jwtService, userMapper, userService, domainEventPublisher
        );

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .role(Role.USER)
                .build();
        when(userService.findUserByEmail("user@example.com")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");

        authService.login(new LoginRequest("user@example.com", "Password123!"));

        ArgumentCaptor<LoginSucceededEvent> captor = ArgumentCaptor.forClass(LoginSucceededEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(user.getId());
        assertThat(captor.getValue().email()).isEqualTo("user@example.com");
    }

    @Test
    void loginMapsDeletedUserRaceToBadCredentialsAndPublishesFailedEvent() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        AuthService authService = newAuthService(
                authenticationManager, jwtService, userMapper, userService, domainEventPublisher
        );

        when(userService.findUserByEmail("gone@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("gone@example.com", "Password123!")))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<LoginFailedEvent> captor = ArgumentCaptor.forClass(LoginFailedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().attemptedEmail()).isEqualTo("gone@example.com");
    }
}

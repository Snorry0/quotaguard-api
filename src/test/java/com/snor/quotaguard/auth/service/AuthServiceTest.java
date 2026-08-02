package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.audit.AuditPublisher;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerCreatesUserOnceAndDoesNotReloadByEmail() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        AuditPublisher auditPublisher = mock(AuditPublisher.class);
        AuthService authService = new AuthService(
                authenticationManager, jwtService, userMapper, userService, auditPublisher
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
    }

    @Test
    void loginMapsDeletedUserRaceToBadCredentials() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        AuditPublisher auditPublisher = mock(AuditPublisher.class);
        AuthService authService = new AuthService(
                authenticationManager, jwtService, userMapper, userService, auditPublisher
        );

        when(userService.findUserByEmail("gone@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("gone@example.com", "Password123!")))
                .isInstanceOf(BadCredentialsException.class);
    }
}

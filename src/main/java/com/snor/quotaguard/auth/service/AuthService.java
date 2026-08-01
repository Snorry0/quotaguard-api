package com.snor.quotaguard.auth.service;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.auth.dto.response.AuthResponse;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.security.JwtService;
import com.snor.quotaguard.user.EmailNormalizer;
import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.mapper.UserMapper;
import com.snor.quotaguard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserService userService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUserEntity(
                new CreateUserRequest(request.email(), request.password(), Role.USER)
        );
        return new AuthResponse(
                jwtService.generateToken(user),
                jwtService.getExpirationInstant(),
                userMapper.toResponse(user)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );
        User user;
        try {
            user = userService.findUserByEmail(normalizedEmail);
        } catch (ResourceNotFoundException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getExpirationInstant(), userMapper.toResponse(user));
    }
}

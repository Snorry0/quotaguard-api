package com.snor.quotaguard.security;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        return getCurrentUserIfPresent()
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user could not be resolved"));
    }

    public Optional<User> getCurrentUserIfPresent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findById(UUID.fromString(authentication.getName()));
    }
}

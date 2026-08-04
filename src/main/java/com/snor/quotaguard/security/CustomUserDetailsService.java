package com.snor.quotaguard.security;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.user.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    /**
     * {@code @Lazy} breaks the container cycle SecurityConfig →
     * CustomUserDetailsService → UserService → PasswordEncoder (declared in
     * SecurityConfig). The proxy resolves to the fully-initialized UserService
     * (including its cache/transaction proxy) on first use at login time.
     */
    public CustomUserDetailsService(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        try {
            user = userService.findUserByEmail(username);   // the @Cacheable("users") service method
        } catch (ResourceNotFoundException ex) {
            throw new UsernameNotFoundException("User not found");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}

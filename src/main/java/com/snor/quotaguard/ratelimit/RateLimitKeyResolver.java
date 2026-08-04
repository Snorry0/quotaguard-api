package com.snor.quotaguard.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the rate-limit key for a request.
 *
 * <p>Authenticated requests are keyed by the authenticated principal (the
 * userId String set by {@code JwtAuthenticationFilter}), so a user's limit
 * follows the user across IP changes. Anonymous requests fall back to the
 * client IP. The {@code AnonymousAuthenticationToken} guard mirrors
 * {@code CurrentUserProvider.getCurrentUserIfPresent()}.</p>
 */
@Component
public class RateLimitKeyResolver {

    private static final String USER_PREFIX = "USER:";
    private static final String IP_PREFIX = "IP:";

    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return USER_PREFIX + authentication.getName();
        }
        return IP_PREFIX + request.getRemoteAddr();
    }
}

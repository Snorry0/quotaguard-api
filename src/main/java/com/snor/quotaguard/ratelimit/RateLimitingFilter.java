package com.snor.quotaguard.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.dto.response.ErrorResponse;
import com.snor.quotaguard.metrics.BusinessMetrics;
import com.snor.quotaguard.ratelimit.config.RateLimitProperties;
import com.snor.quotaguard.ratelimit.store.KeyedBucketStore;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web-layer token-bucket rate limiter for the auth endpoints.
 *
 * <p>Only paths present in {@code quotaguard.rate-limiting.endpoints} are
 * checked ({@link #shouldNotFilter(HttpServletRequest)}); everything else
 * passes through with zero bucket work. On rejection the filter writes the
 * {@code 429} {@link ErrorResponse} directly to the response — it never
 * re-dispatches, so no token is consumed by the rejection itself. The filter
 * lives only in the {@code SecurityFilterChain} (registered via
 * {@code addFilterAfter} after the JWT filter); the {@code FilterRegistrationBean}
 * in {@code SecurityConfig} disables the servlet-container double registration.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final RateLimitKeyResolver keyResolver;
    private final KeyedBucketStore bucketStore;
    private final RateLimitProperties properties;
    private final BusinessMetrics businessMetrics;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.endpoints().containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitProperties.Limit limit = properties.endpoints().get(request.getRequestURI());
        String key = keyResolver.resolve(request);
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.tokens())
                .refillGreedy(limit.tokens(), limit.refillPeriod())
                .build();
        Bucket bucket = bucketStore.getBucket(key, bandwidth);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1,
                (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        businessMetrics.recordRateLimitRejection();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        Map<String, String> details = new LinkedHashMap<>();
        details.put("retryAfterSeconds", String.valueOf(retryAfterSeconds));
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Retry after " + retryAfterSeconds + "s.",
                request.getRequestURI(),
                details
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

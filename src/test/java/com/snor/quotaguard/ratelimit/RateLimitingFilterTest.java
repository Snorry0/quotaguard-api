package com.snor.quotaguard.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snor.quotaguard.dto.response.ErrorResponse;
import com.snor.quotaguard.metrics.BusinessMetrics;
import com.snor.quotaguard.ratelimit.config.RateLimitProperties;
import com.snor.quotaguard.ratelimit.store.KeyedBucketStore;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link RateLimitingFilter}.
 *
 * <p>Deterministic via a {@link TimeMeter} seam: the bucket is built with
 * {@code withCustomTimePrecision(testTimeMeter)} (the shape of Bucket4j's
 * test-scope {@code TimeMeterMock}), so refill behaviour can be advanced by
 * whole seconds without sleeping. The bucket is a 3 tokens / 1 minute greedy
 * bucket — 1 token per 20s — matching the test-profile login limit.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private static final RateLimitProperties PROPERTIES = new RateLimitProperties(Map.of(
            LOGIN_PATH, new RateLimitProperties.Limit(3, Duration.ofMinutes(1)),
            "/api/v1/auth/refresh", new RateLimitProperties.Limit(5, Duration.ofMinutes(1))
    ));

    @Mock
    private RateLimitKeyResolver keyResolver;
    @Mock
    private KeyedBucketStore bucketStore;
    @Mock
    private BusinessMetrics businessMetrics;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private TestTimeMeter testTimeMeter;
    private Bucket bucket;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        testTimeMeter = new TestTimeMeter();
        bucket = Bucket.builder()
                .withCustomTimePrecision(testTimeMeter)
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillGreedy(3, Duration.ofMinutes(1))
                        .build())
                .build();
        // lenient: shouldNotFilterSkipsUnconfiguredPaths never reaches the bucket.
        lenient().when(bucketStore.getBucket(any(), any())).thenReturn(bucket);
        filter = new RateLimitingFilter(objectMapper, keyResolver, bucketStore, PROPERTIES, businessMetrics);
    }

    @Test
    void consumesOneTokenAndContinuesWhenAvailable() throws Exception {
        MockHttpServletRequest request = request(LOGIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
        verify(businessMetrics, never()).recordRateLimitRejection();
    }

    @Test
    void writes429WithRetryAfterWhenExhausted() throws Exception {
        assertThat(bucket.tryConsume(3)).isTrue();

        MockHttpServletRequest request = request(LOGIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);

        String retryAfter = response.getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isGreaterThanOrEqualTo(1);

        ErrorResponse body = objectMapper.readValue(
                response.getContentAsString(StandardCharsets.UTF_8), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.message()).contains("Too many requests");
        assertThat(body.path()).isEqualTo(LOGIN_PATH);
        assertThat(body.validationErrors()).containsEntry("retryAfterSeconds", retryAfter);

        verify(businessMetrics, times(1)).recordRateLimitRejection();
    }

    @Test
    void recoversAfterRefillWindow() throws Exception {
        assertThat(bucket.tryConsume(3)).isTrue();

        MockHttpServletRequest request = request(LOGIN_PATH);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, firstResponse, chain);
        assertThat(firstResponse.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());

        // Greedy 3/min refills 1 token per 20s; 21s guarantees at least one token back.
        testTimeMeter.addSeconds(21);

        filter.doFilterInternal(request, secondResponse, chain);

        verify(chain, times(1)).doFilter(request, secondResponse);
        assertThat(secondResponse.getStatus()).isNotEqualTo(429);
        verify(businessMetrics, times(1)).recordRateLimitRejection();
    }

    @Test
    void shouldNotFilterSkipsUnconfiguredPaths() {
        MockHttpServletRequest unconfigured = new MockHttpServletRequest();
        unconfigured.setRequestURI("/api/v1/quota/reset");

        assertThat(filter.shouldNotFilter(unconfigured)).isTrue();
        assertThat(filter.shouldNotFilter(request(LOGIN_PATH))).isFalse();
    }

    @Test
    void usesTheResolvedKey() throws Exception {
        when(keyResolver.resolve(any())).thenReturn("IP:203.0.113.99");

        MockHttpServletRequest request = request(LOGIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(bucketStore).getBucket(keyCaptor.capture(), any(Bandwidth.class));
        assertThat(keyCaptor.getValue()).isEqualTo(keyResolver.resolve(request));
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setRemoteAddr("203.0.113.1");
        return request;
    }

    /**
     * Deterministic clock for the bucket (the shape of Bucket4j's test-scope
     * {@code TimeMeterMock}): time only advances when {@link #addSeconds(long)}
     * is called, so refill behaviour is fully controlled by the test.
     */
    private static final class TestTimeMeter implements TimeMeter {

        private long currentTimeNanos;

        @Override
        public long currentTimeNanos() {
            return currentTimeNanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }

        void addSeconds(long seconds) {
            currentTimeNanos += seconds * 1_000_000_000L;
        }
    }
}

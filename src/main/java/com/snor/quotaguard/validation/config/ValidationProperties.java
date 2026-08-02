package com.snor.quotaguard.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the shared validation layer, bound under the
 * {@code quotaguard.validation} prefix.
 *
 * <p>The record is registered as a Spring bean through
 * {@code @ConfigurationPropertiesScan} on {@code QuotaGuardApplication}; it is kept
 * separate from {@code QuotaGuardProperties} so each configuration record stays
 * cohesive (same convention as {@code JwtProperties}).</p>
 *
 * <p>Nested records:</p>
 * <ul>
 *   <li>{@link Quota} &mdash; bounds for {@code @ValidQuotaLimit}.</li>
 *   <li>{@link Pagination} &mdash; page/size bounds for {@code @AllowedPageSize}
 *       and future page validation.</li>
 *   <li>{@link Password} &mdash; rules applied by {@code @StrongPassword}.</li>
 * </ul>
 *
 * <p>All values carry defaults in {@code application.yml} and
 * {@code application-test.yml}.</p>
 */
@ConfigurationProperties(prefix = "quotaguard.validation")
public record ValidationProperties(
        Quota quota,
        Pagination pagination,
        Password password
) {

    /**
     * Quota limit bounds, applied by {@code @ValidQuotaLimit}.
     *
     * @param min            inclusive lower bound for a quota limit
     * @param max            inclusive upper bound for a quota limit
     * @param rejectOverflow when {@code true}, also rejects values that would
     *                       overflow downstream arithmetic (see the validator
     *                       JavaDoc for the exact guard)
     */
    public record Quota(
            int min,
            int max,
            boolean rejectOverflow
    ) {
    }

    /**
     * Pagination bounds, applied by {@code @AllowedPageSize} (and, in a later
     * phase, by page validation on controllers).
     *
     * @param minPage     inclusive lower bound for a page number
     * @param maxPage     inclusive upper bound for a page number
     * @param maxSize     inclusive upper bound for a page size
     * @param defaultPage default page number used when omitted
     * @param defaultSize default page size used when omitted
     */
    public record Pagination(
            int minPage,
            int maxPage,
            int maxSize,
            int defaultPage,
            int defaultSize
    ) {
    }

    /**
     * Password strength policy, applied by {@code @StrongPassword}.
     *
     * @param minLength     inclusive minimum password length
     * @param maxLength     inclusive maximum password length
     * @param requireUpper  when {@code true}, at least one uppercase letter
     * @param requireLower  when {@code true}, at least one lowercase letter
     * @param requireDigit  when {@code true}, at least one digit
     * @param requireSpecial when {@code true}, at least one character from
     *                       {@code specialChars}
     * @param specialChars  the set of characters accepted as "special"
     */
    public record Password(
            int minLength,
            int maxLength,
            boolean requireUpper,
            boolean requireLower,
            boolean requireDigit,
            boolean requireSpecial,
            String specialChars
    ) {
    }
}

package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.ValidQuotaLimitValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an {@link Integer} quota limit is within the configured bounds
 * and does not overflow downstream arithmetic.
 *
 * <h2>Semantics</h2>
 * <p>The validator reads the inclusive lower/upper bounds from
 * {@code quotaguard.validation.quota} (see
 * {@link com.snor.quotaguard.validation.config.ValidationProperties.Quota}).
 * When {@code reject-overflow} is enabled, values that would overflow the
 * arithmetic used by downstream quota accounting are rejected as well
 * (see {@link ValidQuotaLimitValidator} for the exact guard).
 * {@code null} is accepted &mdash; pair this annotation with {@code @NotNull}
 * when the limit is mandatory.</p>
 *
 * <p>No public endpoint accepts a client-supplied limit today, so this
 * constraint is forward-looking (for example a future admin endpoint that sets
 * user limits); it is fully self-contained and configuration-driven.</p>
 *
 * <h2>Configuration</h2>
 * <p>Bounds come from {@code quotaguard.validation.quota.min} and
 * {@code .max}; the overflow guard is switched by {@code .reject-overflow}.
 * Defaults: {@code min=1}, {@code max=100000}, {@code reject-overflow=true}.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.ValidQuotaLimit.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @NotNull
 * @ValidQuotaLimit
 * private Integer dailyLimit;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = ValidQuotaLimitValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidQuotaLimit {

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.ValidQuotaLimit.message}.
     */
    String message() default "{com.snor.quotaguard.validation.ValidQuotaLimit.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

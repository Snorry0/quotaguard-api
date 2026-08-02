package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.AllowedPageSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an {@link Integer} page size lies within the allowed range.
 *
 * <h2>Semantics</h2>
 * <p>The validator rejects values below {@link #min()} (default {@code 1},
 * fixed for now) and above the configured
 * {@code quotaguard.validation.pagination.max-size}. {@code null} is accepted
 * &mdash; required-page semantics belong to other constraints (for example
 * {@code @NotNull} in a later phase on the controller).</p>
 *
 * <p>This constraint is meant to run at the controller boundary
 * ({@code @Validated}) so a malformed {@code size} query parameter yields a
 * {@code 400} before it reaches the service layer. The service-side clamping in
 * {@code PageRequestFactory} is retained as defense-in-depth.</p>
 *
 * <h2>Configuration</h2>
 * <p>The upper bound comes from {@code quotaguard.validation.pagination.max-size}
 * (default {@code 100}); the lower bound is the {@link #min()} annotation
 * parameter (default {@code 1}, not configurable for now).</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.AllowedPageSize.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Validated
 * public ResponseEntity<?> list(
 *         @RequestParam @Min(0) int page,
 *         @RequestParam @AllowedPageSize int size) { ... }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = AllowedPageSizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedPageSize {

    /**
     * Inclusive lower bound for the page size. Defaults to {@code 1}; not
     * configurable for now.
     */
    int min() default 1;

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.AllowedPageSize.message}.
     */
    String message() default "{com.snor.quotaguard.validation.AllowedPageSize.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

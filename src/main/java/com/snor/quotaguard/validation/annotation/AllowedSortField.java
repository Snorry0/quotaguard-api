package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.AllowedSortFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a {@link String} sort property is part of an explicit
 * whitelist.
 *
 * <h2>Semantics</h2>
 * <p>The whitelist is the single source of truth: the value is compared
 * case-sensitively and exactly against the {@link #whitelist()} entries and
 * rejected when it does not match one of them. {@code null} and blank values
 * are rejected unconditionally; this constraint must therefore be applied
 * only to <em>required</em> sort parameters (a {@code 400} fires at the
 * boundary). For an optional sort, do not apply this annotation and let the
 * controller apply its own absence semantics (e.g. a default sort).</p>
 *
 * <p>The whitelist also protects the persistence layer: only property names
 * listed here can be passed through, so entity field names are never exposed
 * through the REST API.</p>
 *
 * <h2>Configuration</h2>
 * <p>No configuration; the allowed fields are declared per usage site via
 * {@link #whitelist()}.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.AllowedSortField.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Validated
 * public ResponseEntity<?> list(
 *         @RequestParam
 *         @AllowedSortField(whitelist = {"timestamp", "id", "actor"})
 *         String sort) { ... }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = AllowedSortFieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedSortField {

    /**
     * The exhaustive, case-sensitive list of allowed sort property names.
     * Must contain at least one entry.
     */
    String[] whitelist();

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.AllowedSortField.message}.
     */
    String message() default "{com.snor.quotaguard.validation.AllowedSortField.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

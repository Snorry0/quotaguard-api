package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.TrimmedValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a {@link String} value is already trimmed.
 *
 * <h2>Semantics</h2>
 * <p>The value is rejected when it contains leading or trailing whitespace
 * (i.e. {@code value} differs from {@code value.trim()}) or when it is empty
 * after trimming (blank/whitespace-only input). Internal whitespace is allowed.
 * {@code null} is accepted &mdash; pair this annotation with {@code @NotBlank}
 * when the value is mandatory.</p>
 *
 * <h2>Configuration</h2>
 * <p>No configuration; behaviour is fixed.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.Trimmed.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Trimmed
 * private String displayName;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = TrimmedValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Trimmed {

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.Trimmed.message}.
     */
    String message() default "{com.snor.quotaguard.validation.Trimmed.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

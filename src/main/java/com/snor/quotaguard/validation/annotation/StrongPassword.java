package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a {@link String} password satisfies the configured password
 * policy.
 *
 * <h2>Semantics</h2>
 * <p>The validator reads all rules from
 * {@code quotaguard.validation.password} (see
 * {@link com.snor.quotaguard.validation.config.ValidationProperties.Password}):
 * inclusive {@code min-length}/{@code max-length} bounds, and each of the
 * independent, individually switchable requirements {@code require-upper},
 * {@code require-lower}, {@code require-digit} and {@code require-special}.
 * The set of characters treated as "special" is configurable via
 * {@code special-chars}. {@code null} is always accepted &mdash; pair this
 * annotation with {@code @NotBlank} when the password is mandatory.</p>
 *
 * <h2>Configuration</h2>
 * <p>No annotation parameters; every rule comes from configuration. Defaults
 * (also present in {@code application.yml}): length {@code 8..100}, all four
 * character-class requirements enabled.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.StrongPassword.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @NotBlank
 * @StrongPassword
 * private String password;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.StrongPassword.message}.
     */
    String message() default "{com.snor.quotaguard.validation.StrongPassword.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

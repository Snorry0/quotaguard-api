package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.NormalizedEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a {@link String} email address is already in its canonical
 * normalized form, i.e. exactly {@code value.trim().toLowerCase()}.
 *
 * <h2>Semantics</h2>
 * <p>The value is rejected when it contains whitespace anywhere, when it
 * contains uppercase characters, or when it is not a well-formed email
 * address. {@code null} is accepted and the constraint is composable: pair it
 * with {@code @NotBlank} (or {@code @NotNull}) to make an email field
 * mandatory. This mirrors the standard Bean Validation convention and allows
 * the constraint to be used on optional fields such as
 * {@code UpdateUserRequest.email}, where an absent value is simply skipped.
 * The format check delegates to Hibernate Validator's internal
 * {@code @Email} validator so this constraint can never disagree with the
 * framework's own email grammar. Because whitespace and uppercase are rejected
 * at the boundary, a value that passes here is already normalized and can be
 * persisted as-is.</p>
 *
 * <p>The existing {@code EmailNormalizer} call sites in the services are kept:
 * the normalizer is idempotent, so it remains as defense-in-depth even though
 * the boundary now guarantees normalized input.</p>
 *
 * <h2>Configuration</h2>
 * <p>No configuration; behaviour is fixed.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.NormalizedEmail.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @NormalizedEmail
 * private String email;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = NormalizedEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NormalizedEmail {

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.NormalizedEmail.message}.
     */
    String message() default "{com.snor.quotaguard.validation.NormalizedEmail.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

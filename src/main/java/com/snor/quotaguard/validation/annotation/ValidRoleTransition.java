package com.snor.quotaguard.validation.annotation;

import com.snor.quotaguard.validation.validator.ValidRoleTransitionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a role transition against the configured
 * {@link com.snor.quotaguard.validation.policy.RoleTransitionPolicy}.
 *
 * <h2>Semantics</h2>
 * <p>The constraint describes a specific transition through
 * {@link #fromRole()} and {@link #toRole()}. When either parameter is
 * {@code "ANY"} (the default), the caller deliberately leaves the policy open
 * and the validator only checks that any explicitly named role corresponds to
 * a known {@link com.snor.quotaguard.domain.enums.Role} constant. When both
 * parameters name concrete roles, the validator additionally delegates to the
 * RoleTransitionPolicy bean resolved from the application context and
 * rejects the value when the policy forbids the transition.</p>
 *
 * <p>Today every transition is allowed ({@code PermissiveRoleTransitionPolicy}),
 * so this constraint mainly guarantees that unknown role strings in a
 * transition request are rejected at the boundary instead of surfacing as a
 * {@code 500}. The policy extension point is future-proof: when real rules
 * exist, a new policy bean replaces the permissive one without touching this
 * annotation or its validator.</p>
 *
 * <p>The annotation applies to types (class level, where the default
 * {@code "ANY"/"ANY"} leaves the policy open for the whole payload), to fields
 * and to method parameters.</p>
 *
 * <h2>Configuration</h2>
 * <p>No configuration; the policy comes from the
 * {@link com.snor.quotaguard.validation.policy.RoleTransitionPolicy} bean.</p>
 *
 * <h2>Default message</h2>
 * <p>{@code {com.snor.quotaguard.validation.ValidRoleTransition.message}}</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @ValidRoleTransition(fromRole = "ADMIN", toRole = "USER")
 * private Role newRole;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = ValidRoleTransitionValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoleTransition {

    /**
     * The source role of the described transition, or {@code "ANY"} to leave
     * the policy open on the source side. When not {@code "ANY"}, the value
     * must name a known {@code Role} enum constant.
     */
    String fromRole() default "ANY";

    /**
     * The target role of the described transition, or {@code "ANY"} to leave
     * the policy open on the target side. When not {@code "ANY"}, the value
     * must name a known {@code Role} enum constant.
     */
    String toRole() default "ANY";

    /**
     * The default message; resolved from the {@code ValidationMessages} bundle
     * key {@code com.snor.quotaguard.validation.ValidRoleTransition.message}.
     */
    String message() default "{com.snor.quotaguard.validation.ValidRoleTransition.message}";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload used by validation clients.
     */
    Class<? extends Payload>[] payload() default {};
}

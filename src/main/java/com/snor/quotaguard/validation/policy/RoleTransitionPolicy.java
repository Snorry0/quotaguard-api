package com.snor.quotaguard.validation.policy;

import com.snor.quotaguard.domain.enums.Role;

/**
 * Extension point for role-transition rules.
 *
 * <p>Today every transition is allowed, so the default implementation
 * ({@link PermissiveRoleTransitionPolicy}) returns {@code true} for all inputs.
 * As soon as real transition rules exist (for example {@code ADMIN -> USER}
 * allowed but {@code USER -> ADMIN} forbidden), a new {@code @Component}
 * implementation of this interface replaces the permissive one without touching
 * the {@code @ValidRoleTransition} validator, which simply delegates here.</p>
 *
 * <p>The policy is resolved by the validator through Spring's
 * {@code SpringConstraintValidatorFactory}, so the bean implementing this
 * interface is picked up from the application context automatically.</p>
 */
public interface RoleTransitionPolicy {

    /**
     * Decides whether a transition from {@code from} to {@code to} is allowed.
     *
     * @param from the source role, never {@code null}
     * @param to   the target role, never {@code null}
     * @return {@code true} when the transition is allowed, {@code false} otherwise
     */
    boolean isAllowed(Role from, Role to);
}

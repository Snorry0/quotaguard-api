package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.annotation.ValidRoleTransition;
import com.snor.quotaguard.validation.policy.RoleTransitionPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for {@link ValidRoleTransition}.
 *
 * <p>When either {@code fromRole} or {@code toRole} is {@code "ANY"}, the
 * caller leaves the policy open and the value is accepted without further
 * checks. When both name concrete roles, the validator first verifies each name
 * corresponds to a known {@link Role} enum constant (an unknown role string is
 * rejected at the boundary instead of surfacing later as a {@code 500}) and
 * then delegates the actual transition decision to the
 * {@link RoleTransitionPolicy} bean resolved from the application context.</p>
 *
 * <p>Today all transitions are allowed, so under the default permissive policy
 * any transition that names two known roles passes. The policy bean is
 * replaceable, which makes this constraint future-proof without touching the
 * annotation or this validator.</p>
 *
 * <p>The instance is created by Spring's {@code SpringConstraintValidatorFactory}
 * (automatic with {@code spring-boot-starter-validation}), so the
 * {@link RoleTransitionPolicy} collaborator is injected via {@code @Autowired}.</p>
 */
public class ValidRoleTransitionValidator implements ConstraintValidator<ValidRoleTransition, Object> {

    private String fromRole;
    private String toRole;

    @Autowired
    private RoleTransitionPolicy roleTransitionPolicy;

    @Override
    public void initialize(ValidRoleTransition annotation) {
        this.fromRole = annotation.fromRole();
        this.toRole = annotation.toRole();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Any explicitly named role must correspond to a known Role constant,
        // even when the other end is "ANY"; a typo (e.g. fromRole = "BOGUS")
        // would otherwise pass silently instead of failing fast at the boundary.
        if (!"ANY".equals(fromRole) && resolve(fromRole) == null) {
            return false;
        }
        if (!"ANY".equals(toRole) && resolve(toRole) == null) {
            return false;
        }
        if ("ANY".equals(fromRole) || "ANY".equals(toRole)) {
            return true;
        }
        Role from = resolve(fromRole);
        Role to = resolve(toRole);
        return roleTransitionPolicy.isAllowed(from, to);
    }

    private Role resolve(String name) {
        for (Role role : Role.values()) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return null;
    }
}

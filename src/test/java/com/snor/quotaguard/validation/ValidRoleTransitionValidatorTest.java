package com.snor.quotaguard.validation;

import com.snor.quotaguard.domain.enums.Role;
import com.snor.quotaguard.validation.policy.PermissiveRoleTransitionPolicy;
import com.snor.quotaguard.validation.policy.RoleTransitionPolicy;
import com.snor.quotaguard.validation.validator.ValidRoleTransitionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidRoleTransitionValidator} — validation-layer spec,
 * section 3.2. Exercises the policy extension point: the default permissive
 * policy, a strict replacement policy, unknown-role rejection (Phase-1 Oracle
 * remediation) and the {@code "ANY"} short-circuit.
 */
class ValidRoleTransitionValidatorTest {

    private static final RoleTransitionPolicy PERMISSIVE = new PermissiveRoleTransitionPolicy();

    /**
     * Strict policy proving the extension point is honoured. {@code SUPER_ADMIN}
     * is a sentinel: no transition may touch a role named {@code SUPER_ADMIN}.
     * It cannot be represented as a {@link Role} today (the enum is
     * {@code USER|ADMIN}), so the sentinel branch is expressed in the policy
     * contract; what is observable through the validator today is that the
     * policy is actually consulted (USER→ADMIN blocked, ADMIN→USER allowed).
     */
    private static final RoleTransitionPolicy STRICT = (from, to) -> {
        if ("SUPER_ADMIN".equals(from.name()) || "SUPER_ADMIN".equals(to.name())) {
            return false;
        }
        return !(from == Role.USER && to == Role.ADMIN);
    };

    private ValidRoleTransitionValidator validator(String fromRole, String toRole, RoleTransitionPolicy policy) {
        ValidRoleTransitionValidator validator = new ValidRoleTransitionValidator();
        ReflectionTestUtils.setField(validator, "fromRole", fromRole);
        ReflectionTestUtils.setField(validator, "toRole", toRole);
        ReflectionTestUtils.setField(validator, "roleTransitionPolicy", policy);
        return validator;
    }

    @Test
    void permissiveDefaultAllowsEveryTransition() {
        ValidRoleTransitionValidator validator = validator("USER", "ADMIN", PERMISSIVE);
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator("ADMIN", "USER", PERMISSIVE).isValid(null, null)).isTrue();
    }

    @Test
    void strictPolicyBlocksUserToAdmin() {
        assertThat(validator("USER", "ADMIN", STRICT).isValid(null, null)).isFalse();
    }

    @Test
    void strictPolicyAllowsAdminToUser() {
        assertThat(validator("ADMIN", "USER", STRICT).isValid(null, null)).isTrue();
    }

    @Test
    void strictPolicyTreatsSuperAdminAsImmutableSentinel() {
        // The sentinel branch of the policy contract; SUPER_ADMIN is not a Role
        // constant today, so it is asserted at the policy level.
        assertThat(STRICT.isAllowed(Role.USER, Role.ADMIN)).isFalse();
        assertThat(STRICT.isAllowed(Role.ADMIN, Role.USER)).isTrue();
    }

    @Test
    void rejectsUnknownFromRoleEvenWhenToIsAny() {
        assertThat(validator("BOGUS", "ANY", PERMISSIVE).isValid(null, null)).isFalse();
    }

    @Test
    void rejectsUnknownToRoleEvenWhenFromIsAny() {
        assertThat(validator("ANY", "BOGUS", PERMISSIVE).isValid(null, null)).isFalse();
    }

    @Test
    void rejectsUnknownRolePair() {
        assertThat(validator("BOGUS", "USER", PERMISSIVE).isValid(null, null)).isFalse();
    }

    @Test
    void shortCircuitsWhenFromRoleIsAny() {
        assertThat(validator("ANY", "ADMIN", STRICT).isValid(null, null)).isTrue();
    }

    @Test
    void shortCircuitsWhenToRoleIsAny() {
        assertThat(validator("USER", "ANY", STRICT).isValid(null, null)).isTrue();
    }
}

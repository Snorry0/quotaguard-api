package com.snor.quotaguard.validation.policy;

import com.snor.quotaguard.domain.enums.Role;
import org.springframework.stereotype.Component;

/**
 * Default {@link RoleTransitionPolicy} used while every role transition is
 * allowed. Returns {@code true} unconditionally.
 *
 * <p>When real transition rules are introduced, replace this bean with a
 * dedicated implementation; the {@code @ValidRoleTransition} validator requires
 * no change because it only depends on the {@link RoleTransitionPolicy}
 * interface.</p>
 */
@Component
public class PermissiveRoleTransitionPolicy implements RoleTransitionPolicy {

    @Override
    public boolean isAllowed(Role from, Role to) {
        return true;
    }
}

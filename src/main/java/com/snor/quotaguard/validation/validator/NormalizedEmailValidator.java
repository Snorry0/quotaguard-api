package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.NormalizedEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link NormalizedEmail}.
 *
 * <p>Rejects whitespace anywhere, uppercase characters and malformed addresses;
 * {@code null} is accepted and the constraint is composable &mdash; pair it
 * with {@code @NotBlank} (or {@code @NotNull}) to make an email field
 * mandatory. This follows the standard Bean Validation convention and is what
 * allows the constraint to be used on optional fields such as
 * {@code UpdateUserRequest.email}, where an absent value must simply be
 * skipped. The format check is delegated to Hibernate Validator's own
 * {@code @Email} implementation
 * ({@link org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator})
 * so this constraint can never diverge from the framework's email grammar; the
 * delegate is instantiated directly and never initialised, so it uses its
 * default grammar (the same one {@code @Email} applies out of the box).</p>
 *
 * <p><strong>Versioning note.</strong> The delegate lives under
 * {@code org.hibernate.validator.internal.*} and may move or change across
 * Hibernate Validator major versions. This delegation was verified against
 * hibernate-validator 8.0.1.Final; any upgrade of {@code hibernate-validator}
 * (and any change of major version) must re-verify the delegate before this
 * class is shipped.</p>
 *
 * <p>Because whitespace and uppercase are rejected here, a passing value is
 * already in the canonical normalized form {@code trim().toLowerCase()} and can
 * be persisted as-is; the existing {@code EmailNormalizer} remains as
 * idempotent defense-in-depth.</p>
 */
public class NormalizedEmailValidator implements ConstraintValidator<NormalizedEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        if (!value.equals(value.toLowerCase())) {
            return false;
        }
        return new org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator()
                .isValid(value, context);
    }
}

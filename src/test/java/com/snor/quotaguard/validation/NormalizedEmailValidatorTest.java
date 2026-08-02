package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.validator.NormalizedEmailValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NormalizedEmailValidator} — validation-layer spec, section 3.2.
 */
class NormalizedEmailValidatorTest {

    private final NormalizedEmailValidator validator = new NormalizedEmailValidator();

    @Test
    void acceptsNormalizedEmail() {
        assertThat(validator.isValid("a@b.com", null)).isTrue();
        assertThat(validator.isValid("a.b+c@sub.example.co", null)).isTrue();
    }

    @Test
    void acceptsSingleLabelDomain() {
        // Hibernate Validator 8.0.1's email grammar accepts single-label domains
        // (the dot-group in EMAIL_DOMAIN_PATTERN is optional), so "a@b" is valid.
        assertThat(validator.isValid("a@b", null)).isTrue();
    }

    @Test
    void acceptsNull() {
        // Null is accepted (Phase-2 remediation): the constraint is composable
        // with @NotBlank/@NotNull and is used on the optional
        // UpdateUserRequest.email, where an omitted value means "no change".
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejectsEmailWithLeadingAndTrailingWhitespace() {
        assertThat(validator.isValid(" A@B.COM ", null)).isFalse();
    }

    @Test
    void rejectsUppercaseEmail() {
        assertThat(validator.isValid("A@B.COM", null)).isFalse();
    }

    @Test
    void rejectsEmailWithInternalWhitespace() {
        assertThat(validator.isValid("a@b .com", null)).isFalse();
    }

    @Test
    void rejectsNonEmailString() {
        assertThat(validator.isValid("not-an-email", null)).isFalse();
    }
}

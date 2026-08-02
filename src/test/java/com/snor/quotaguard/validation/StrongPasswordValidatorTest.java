package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.config.ValidationProperties;
import com.snor.quotaguard.validation.validator.StrongPasswordValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StrongPasswordValidator} — validation-layer spec, section 3.2.
 */
class StrongPasswordValidatorTest {

    private static final ValidationProperties DEFAULTS = new ValidationProperties(
            new ValidationProperties.Quota(1, 100_000, true),
            new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
            new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
    );

    private StrongPasswordValidator validator() {
        StrongPasswordValidator validator = new StrongPasswordValidator();
        ReflectionTestUtils.setField(validator, "validationProperties", DEFAULTS);
        return validator;
    }

    @Test
    void acceptsStrongPassword() {
        assertThat(validator().isValid("Password123!", null)).isTrue();
        assertThat(validator().isValid("Abcdef1@", null)).isTrue();
    }

    @Test
    void acceptsNull() {
        assertThat(validator().isValid(null, null)).isTrue();
    }

    @Test
    void rejectsPasswordMissingAllCharacterClasses() {
        assertThat(validator().isValid("password", null)).isFalse();
    }

    @Test
    void rejectsNumericOnlyPassword() {
        assertThat(validator().isValid("12345678", null)).isFalse();
    }

    @Test
    void rejectsUppercaseOnlyPassword() {
        assertThat(validator().isValid("PASSWORD", null)).isFalse();
    }

    @Test
    void rejectsPasswordShorterThanMinLength() {
        assertThat(validator().isValid("Ab1!", null)).isFalse();
    }

    @Test
    void rejectsPasswordLongerThanMaxLength() {
        assertThat(validator().isValid("Abcdef1@" + "x".repeat(93), null)).isFalse();
    }

    @Test
    void rejectsPasswordMissingUppercase() {
        assertThat(validator().isValid("abcdef1@", null)).isFalse();
    }

    @Test
    void rejectsPasswordMissingLowercase() {
        assertThat(validator().isValid("ABCDEF1@", null)).isFalse();
    }

    @Test
    void rejectsPasswordMissingDigit() {
        assertThat(validator().isValid("Abcdefg@", null)).isFalse();
    }

    @Test
    void rejectsPasswordMissingSpecialCharacter() {
        assertThat(validator().isValid("Abcdef12", null)).isFalse();
    }
}

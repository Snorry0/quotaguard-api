package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.config.ValidationProperties;
import com.snor.quotaguard.validation.validator.ValidQuotaLimitValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidQuotaLimitValidator} — validation-layer spec, section 3.2.
 */
class ValidQuotaLimitValidatorTest {

    private static final ValidationProperties DEFAULTS = new ValidationProperties(
            new ValidationProperties.Quota(1, 100_000, true),
            new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
            new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
    );

    private ValidQuotaLimitValidator validator(ValidationProperties properties) {
        ValidQuotaLimitValidator validator = new ValidQuotaLimitValidator();
        ReflectionTestUtils.setField(validator, "validationProperties", properties);
        return validator;
    }

    @Test
    void acceptsValidLimits() {
        ValidQuotaLimitValidator validator = validator(DEFAULTS);
        assertThat(validator.isValid(1, null)).isTrue();
        assertThat(validator.isValid(1000, null)).isTrue();
        assertThat(validator.isValid(50_000, null)).isTrue();
    }

    @Test
    void acceptsNull() {
        assertThat(validator(DEFAULTS).isValid(null, null)).isTrue();
    }

    @Test
    void rejectsBelowMin() {
        ValidQuotaLimitValidator validator = validator(DEFAULTS);
        assertThat(validator.isValid(0, null)).isFalse();
        assertThat(validator.isValid(-1, null)).isFalse();
    }

    @Test
    void rejectsAboveMax() {
        assertThat(validator(DEFAULTS).isValid(100_001, null)).isFalse();
    }

    @Test
    void rejectsIntegerMaxValueViaOverflowGuard() {
        // Doubling Integer.MAX_VALUE would overflow the int arithmetic used
        // downstream, so the rejectOverflow guard rejects it.
        assertThat(validator(DEFAULTS).isValid(Integer.MAX_VALUE, null)).isFalse();
    }

    @Test
    void honorsConfigurableMax() {
        ValidationProperties narrow = new ValidationProperties(
                new ValidationProperties.Quota(1, 100, true),
                new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
                new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
        );
        ValidQuotaLimitValidator validator = validator(narrow);
        assertThat(validator.isValid(100, null)).isTrue();
        assertThat(validator.isValid(101, null)).isFalse();
    }

    @Test
    void overflowGuardCanBeDisabled() {
        ValidationProperties noOverflowGuard = new ValidationProperties(
                new ValidationProperties.Quota(1, Integer.MAX_VALUE, false),
                new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
                new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
        );
        assertThat(validator(noOverflowGuard).isValid(2_000_000_000, null)).isTrue();

        ValidationProperties withOverflowGuard = new ValidationProperties(
                new ValidationProperties.Quota(1, Integer.MAX_VALUE, true),
                new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
                new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
        );
        assertThat(validator(withOverflowGuard).isValid(2_000_000_000, null)).isFalse();
    }
}

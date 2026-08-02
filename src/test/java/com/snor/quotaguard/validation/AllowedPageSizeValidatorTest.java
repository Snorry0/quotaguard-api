package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.config.ValidationProperties;
import com.snor.quotaguard.validation.validator.AllowedPageSizeValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AllowedPageSizeValidator} — validation-layer spec, section 3.2.
 */
class AllowedPageSizeValidatorTest {

    private static final ValidationProperties PROPS = new ValidationProperties(
            new ValidationProperties.Quota(1, 100_000, true),
            new ValidationProperties.Pagination(0, 100_000, 100, 0, 20),
            new ValidationProperties.Password(8, 100, true, true, true, true, "!@#$%^&*()")
    );

    private AllowedPageSizeValidator validator() {
        AllowedPageSizeValidator validator = new AllowedPageSizeValidator();
        // min comes from the annotation's min() parameter, default 1.
        ReflectionTestUtils.setField(validator, "min", 1);
        ReflectionTestUtils.setField(validator, "validationProperties", PROPS);
        return validator;
    }

    @Test
    void acceptsValidSizes() {
        AllowedPageSizeValidator validator = validator();
        assertThat(validator.isValid(1, null)).isTrue();
        assertThat(validator.isValid(50, null)).isTrue();
        assertThat(validator.isValid(100, null)).isTrue();
    }

    @Test
    void rejectsBelowMin() {
        AllowedPageSizeValidator validator = validator();
        assertThat(validator.isValid(0, null)).isFalse();
        assertThat(validator.isValid(-1, null)).isFalse();
    }

    @Test
    void rejectsAboveMax() {
        assertThat(validator().isValid(101, null)).isFalse();
    }

    @Test
    void acceptsNull() {
        assertThat(validator().isValid(null, null)).isTrue();
    }
}

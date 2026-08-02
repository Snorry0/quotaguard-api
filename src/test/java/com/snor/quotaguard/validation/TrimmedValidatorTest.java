package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.validator.TrimmedValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TrimmedValidator} — validation-layer spec, section 3.2.
 */
class TrimmedValidatorTest {

    private final TrimmedValidator validator = new TrimmedValidator();

    @Test
    void acceptsPlainValue() {
        assertThat(validator.isValid("hello", null)).isTrue();
    }

    @Test
    void acceptsValueWithInternalWhitespace() {
        assertThat(validator.isValid("hello world", null)).isTrue();
    }

    @Test
    void acceptsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejectsLeadingWhitespace() {
        assertThat(validator.isValid(" hello", null)).isFalse();
    }

    @Test
    void rejectsTrailingWhitespace() {
        assertThat(validator.isValid("hello ", null)).isFalse();
    }

    @Test
    void rejectsWhitespaceOnlyValue() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @Test
    void rejectsEmptyValue() {
        assertThat(validator.isValid("", null)).isFalse();
    }
}

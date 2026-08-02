package com.snor.quotaguard.validation;

import com.snor.quotaguard.validation.validator.AllowedSortFieldValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AllowedSortFieldValidator} — validation-layer spec, section 3.2.
 * The whitelist mirrors the Oracle-corrected JPA entity property names used by
 * {@code AuditService.ALLOWED_SORT_PROPERTIES}.
 */
class AllowedSortFieldValidatorTest {

    private static final String[] WHITELIST =
            {"timestamp", "id", "action", "resourceType", "actorEmail"};

    private AllowedSortFieldValidator validator() {
        AllowedSortFieldValidator validator = new AllowedSortFieldValidator();
        ReflectionTestUtils.setField(validator, "whitelist", WHITELIST);
        return validator;
    }

    @Test
    void acceptsWhitelistedField() {
        AllowedSortFieldValidator validator = validator();
        assertThat(validator.isValid("timestamp", null)).isTrue();
        assertThat(validator.isValid("actorEmail", null)).isTrue();
    }

    @Test
    void rejectsDifferentCase() {
        assertThat(validator().isValid("Timestamp", null)).isFalse();
    }

    @Test
    void rejectsLeadingWhitespace() {
        assertThat(validator().isValid(" timestamp", null)).isFalse();
    }

    @Test
    void rejectsTrailingWhitespace() {
        assertThat(validator().isValid("timestamp ", null)).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(validator().isValid(null, null)).isFalse();
    }

    @Test
    void rejectsEmptyValue() {
        assertThat(validator().isValid("", null)).isFalse();
    }

    @Test
    void rejectsFieldNotInWhitelist() {
        assertThat(validator().isValid("bogus", null)).isFalse();
    }
}

package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.Trimmed;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link Trimmed}.
 *
 * <p>Rejects values that contain leading or trailing whitespace (the value
 * differs from its trimmed form) and values that are blank after trimming.
 * Internal whitespace is allowed. {@code null} is valid; pair with
 * {@code @NotBlank} for mandatory fields.</p>
 */
public class TrimmedValidator implements ConstraintValidator<Trimmed, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.isBlank()) {
            return false;
        }
        return value.equals(value.trim());
    }
}

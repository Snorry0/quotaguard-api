package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.AllowedSortField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link AllowedSortField}.
 *
 * <p>Compares the input value case-sensitively and exactly against the
 * annotation's {@code whitelist()}. The whitelist is the single source of
 * truth: {@code null} and blank values are rejected unconditionally. This
 * validator must only be applied to required sort parameters; an optional
 * sort must be handled at the controller (default value, absence semantics)
 * rather than passing {@code null} through here.</p>
 */
public class AllowedSortFieldValidator implements ConstraintValidator<AllowedSortField, String> {

    private String[] whitelist;

    @Override
    public void initialize(AllowedSortField annotation) {
        this.whitelist = annotation.whitelist();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (String allowed : whitelist) {
            if (allowed.equals(value)) {
                return true;
            }
        }
        return false;
    }
}

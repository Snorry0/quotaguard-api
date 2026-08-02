package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.AllowedPageSize;
import com.snor.quotaguard.validation.config.ValidationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for {@link AllowedPageSize}.
 *
 * <p>Accepts values between the annotation's {@code min()} (default
 * {@code 1}) and the configured {@code quotaguard.validation.pagination.max-size}.
 * {@code null} is valid; required-page semantics belong to other constraints
 * (for example {@code @NotNull} at the controller boundary).</p>
 *
 * <p>The instance is created by Spring's {@code SpringConstraintValidatorFactory}
 * (automatic with {@code spring-boot-starter-validation}), so the
 * {@code ValidationProperties} collaborator is injected via {@code @Autowired}.</p>
 */
public class AllowedPageSizeValidator implements ConstraintValidator<AllowedPageSize, Integer> {

    private int min;

    @Autowired
    private ValidationProperties validationProperties;

    @Override
    public void initialize(AllowedPageSize annotation) {
        this.min = annotation.min();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int max = validationProperties.pagination().maxSize();
        return value >= min && value <= max;
    }
}

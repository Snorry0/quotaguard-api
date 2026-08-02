package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.ValidQuotaLimit;
import com.snor.quotaguard.validation.config.ValidationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for {@link ValidQuotaLimit}.
 *
 * <p>Applies the configurable inclusive bounds from
 * {@code ValidationProperties.quota()} ({@code min}/{@code max}) and, when
 * {@code rejectOverflow} is enabled, an explicit overflow guard: values above
 * {@code Integer.MAX_VALUE / 2} are rejected because doubling such a limit
 * would overflow the {@code int} arithmetic used downstream. This guard also
 * rejects {@code Integer.MAX_VALUE} itself. {@code null} is valid; pair with
 * {@code @NotNull} for mandatory fields.</p>
 *
 * <p>The instance is created by Spring's {@code SpringConstraintValidatorFactory}
 * (automatic with {@code spring-boot-starter-validation}), so the
 * {@code ValidationProperties} collaborator is injected via {@code @Autowired}.</p>
 */
public class ValidQuotaLimitValidator implements ConstraintValidator<ValidQuotaLimit, Integer> {

    @Autowired
    private ValidationProperties validationProperties;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        ValidationProperties.Quota quota = validationProperties.quota();
        if (value < quota.min() || value > quota.max()) {
            return false;
        }
        if (quota.rejectOverflow() && value > Integer.MAX_VALUE / 2) {
            return false;
        }
        return true;
    }
}

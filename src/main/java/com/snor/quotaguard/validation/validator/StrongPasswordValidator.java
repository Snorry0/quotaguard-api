package com.snor.quotaguard.validation.validator;

import com.snor.quotaguard.validation.annotation.StrongPassword;
import com.snor.quotaguard.validation.config.ValidationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for {@link StrongPassword}.
 *
 * <p>Applies every rule from {@code ValidationProperties.password()}:
 * inclusive length bounds and each independently switchable character-class
 * requirement. The special-character set is read from configuration, so the
 * check is a plain containment lookup rather than a compiled character class
 * (avoids regex-escaping pitfalls with configurable special characters).
 * {@code null} is valid; pair with {@code @NotBlank} for mandatory fields.</p>
 *
 * <p>The instance is created by Spring's {@code SpringConstraintValidatorFactory}
 * (automatic with {@code spring-boot-starter-validation}), so the
 * {@code ValidationProperties} collaborator is injected via {@code @Autowired}.</p>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Autowired
    private ValidationProperties validationProperties;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        ValidationProperties.Password password = validationProperties.password();
        if (value.length() < password.minLength() || value.length() > password.maxLength()) {
            return false;
        }
        if (password.requireUpper() && value.chars().noneMatch(Character::isUpperCase)) {
            return false;
        }
        if (password.requireLower() && value.chars().noneMatch(Character::isLowerCase)) {
            return false;
        }
        if (password.requireDigit() && value.chars().noneMatch(Character::isDigit)) {
            return false;
        }
        if (password.requireSpecial() && !containsSpecialChar(value, password.specialChars())) {
            return false;
        }
        return true;
    }

    private boolean containsSpecialChar(String value, String specialChars) {
        for (int i = 0; i < value.length(); i++) {
            if (specialChars.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}

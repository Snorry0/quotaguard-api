package com.snor.quotaguard.dto.response;

/**
 * Structured per-field validation failure, carried under the optional
 * {@code errors} list of {@link ErrorResponse}.
 *
 * <p>Fields mirror the requirement that every validation error includes the
 * field name, the rejected value and a human-readable message. The legacy
 * {@code validationErrors: Map<String, String>} (field &rarr; message) remains
 * populated on the same response for backward compatibility.</p>
 *
 * @param field         the property or parameter that failed validation
 * @param rejectedValue the value that was rejected (may be {@code null} for
 *                      null-rejecting constraints)
 * @param message       the resolved validation message
 */
public record FieldErrorDetail(
        String field,
        Object rejectedValue,
        String message
) {
}

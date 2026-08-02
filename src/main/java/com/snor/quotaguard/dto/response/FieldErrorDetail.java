package com.snor.quotaguard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(
        description = """
                Structured per-field validation failure, carried under the `errors` list of
                `ErrorResponse`. Contains the field name, the rejected value and a human-readable
                message.
                """
)
public record FieldErrorDetail(
        @Schema(
                description = "Name of the field that failed validation. For method-level violations, "
                        + "the parameter name.",
                example = "email"
        )
        String field,
        @Schema(
                description = "The value that was rejected. Raw type (may be string, number, object, etc.).",
                example = "Demo@Example.COM"
        )
        Object rejectedValue,
        @Schema(
                description = "Validation message describing the failure.",
                example = "Email must be provided in normalized form: trimmed, lowercase and a well-formed address"
        )
        String message
) {
}

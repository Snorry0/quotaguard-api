package com.snor.quotaguard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error envelope returned for every error status.
 *
 * <p>Contract for the two detail fields:</p>
 * <ul>
 *   <li>On validation failures (HTTP 400), both {@code validationErrors} and {@code errors}
 *       are populated together. {@code validationErrors} maps each field name to its validation
 *       message; {@code errors} is the structured list of per-field details.</li>
 *   <li>On non-validation errors, only the base fields ({@code timestamp}, {@code status},
 *       {@code error}, {@code message}, {@code path}) are populated; {@code validationErrors}
 *       and {@code errors} are {@code null}.</li>
 * </ul>
 */
@Schema(
        description = """
                Standard error envelope returned for all error statuses.

                On validation failures (HTTP 400), both `validationErrors` and `errors` are populated
                together: `validationErrors` maps each field name to its validation message, while
                `errors` is the structured list of per-field details (field, rejected value, message).

                On non-validation errors, only the base fields (`timestamp`, `status`, `error`,
                `message`, `path`) are populated; `validationErrors` and `errors` are `null`.
                """
)
public record ErrorResponse(
        @Schema(
                description = "Server-side timestamp (ISO 8601) of when the response was produced."
        )
        Instant timestamp,
        @Schema(
                description = "HTTP status code, mirrors the response status line.",
                example = "400"
        )
        int status,
        @Schema(
                description = "HTTP status reason phrase.",
                example = "Bad Request"
        )
        String error,
        @Schema(
                description = "Human-readable summary of the error.",
                example = "Validation failed"
        )
        String message,
        @Schema(
                description = "Request path that produced the error.",
                example = "/api/v1/auth/register"
        )
        String path,
        @Schema(
                description = "Map of field name to validation message. "
                        + "Populated together with `errors` on validation failures (HTTP 400). "
                        + "Null on non-validation errors. Legacy field; prefer `errors` for per-field detail."
        )
        Map<String, String> validationErrors,
        @Schema(
                description = "List of per-field error details with the rejected value and message. "
                        + "Populated together with `validationErrors` on validation failures (HTTP 400). "
                        + "Null on non-validation errors."
        )
        List<FieldErrorDetail> errors
) {
    public static ErrorResponse of(HttpStatus status, String message, String path, Map<String, String> details) {
        return of(status, message, path, details, null);
    }

    public static ErrorResponse of(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> details,
            List<FieldErrorDetail> errors
    ) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details,
                errors
        );
    }
}

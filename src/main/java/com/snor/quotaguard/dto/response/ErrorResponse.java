package com.snor.quotaguard.dto.response;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors,
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

package com.snor.quotaguard.dto.response;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(HttpStatus status, String message, String path, Map<String, String> details) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
    }
}

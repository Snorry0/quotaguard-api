package com.snor.quotaguard.exception;

import com.snor.quotaguard.dto.response.ErrorResponse;
import com.snor.quotaguard.dto.response.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleConflict(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(QuotaExceededException.class)
    ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("dailyLimit", String.valueOf(ex.getDailyLimit()));
        details.put("usedToday", String.valueOf(ex.getUsedToday()));
        details.put("attemptedAmount", String.valueOf(ex.getAttemptedAmount()));
        details.put("penaltyType", ex.getPenaltyType().name());
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, details);
    }

    @ExceptionHandler(ActivePenaltyException.class)
    ResponseEntity<ErrorResponse> handleActivePenalty(ActivePenaltyException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("penaltyType", ex.getPenaltyType().name());
        details.put("endsAt", ex.getEndsAt().toString());

        HttpHeaders headers = new HttpHeaders();
        long retryAfterSeconds = Math.max(1, Duration.between(LocalDateTime.now(ZoneOffset.UTC), ex.getEndsAt()).toSeconds());
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ErrorResponse response = ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI(), details);
        return new ResponseEntity<>(response, headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            validationErrors.put(error.getField(), error.getDefaultMessage());
            errors.add(new FieldErrorDetail(error.getField(), error.getRejectedValue(), error.getDefaultMessage()));
        });
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = lastPathNode(violation.getPropertyPath().toString());
            Object rejectedValue = violation.getInvalidValue();
            String message = violation.getMessage();
            validationErrors.put(field, message);
            errors.add(new FieldErrorDetail(field, rejectedValue, message));
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getAllValidationResults()) {
            String field = result.getMethodParameter().getParameterName();
            Object rejectedValue = result.getArgument();
            for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
                String message = resolvable.getDefaultMessage();
                validationErrors.put(field, message);
                errors.add(new FieldErrorDetail(field, rejectedValue, message));
            }
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors, errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Rejected bad request {}: {}", request.getRequestURI(), ex.getMessage());
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Malformed request";
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Rejected bad request {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request", request, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                "The resource was modified concurrently. Please retry the request.",
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnhandled(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing request {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, null);
    }

    @ExceptionHandler(ActiveSessionAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleActiveSession(
            ActiveSessionAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("sessionId", ex.getSessionId().toString());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    @ExceptionHandler(InvalidSessionStateException.class)
    ResponseEntity<ErrorResponse> handleInvalidSessionState(
            InvalidSessionStateException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(SelfDeletionNotAllowedException.class)
    ResponseEntity<ErrorResponse> handleSelfDeletion(
            SelfDeletionNotAllowedException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        return build(status, message, request, details, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details,
            List<FieldErrorDetail> errors
    ) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI(), details, errors));
    }

    private String lastPathNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}

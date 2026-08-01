package com.snor.quotaguard.exception;

import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockingFailureMapsToConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/quota");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException(UserQuota.class, UUID.randomUUID()),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).contains("modified concurrently");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/quota");
    }
}

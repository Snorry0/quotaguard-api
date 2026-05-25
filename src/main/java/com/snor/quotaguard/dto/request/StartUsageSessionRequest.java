package com.snor.quotaguard.dto.request;

import jakarta.validation.constraints.Size;

public record StartUsageSessionRequest(
        @Size(max = 128)
        String clientReference,

        @Size(max = 5000)
        String metadata
) {
}
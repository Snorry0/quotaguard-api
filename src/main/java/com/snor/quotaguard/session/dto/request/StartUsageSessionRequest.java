package com.snor.quotaguard.session.dto.request;

import com.snor.quotaguard.validation.annotation.Trimmed;
import jakarta.validation.constraints.Size;

public record StartUsageSessionRequest(
        @Size(max = 128)
        @Trimmed
        String clientReference,

        @Size(max = 5000)
        @Trimmed
        String metadata
) {
}

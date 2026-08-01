package com.snor.quotaguard.session.dto.response;

import com.snor.quotaguard.domain.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsageSessionResponse(
        UUID id,
        UUID userId,
        String clientReference,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationSeconds,
        Integer amountConsumed,
        SessionStatus status,
        String metadata
) {
}
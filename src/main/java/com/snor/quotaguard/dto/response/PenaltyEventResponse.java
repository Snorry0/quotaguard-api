package com.snor.quotaguard.dto.response;

import com.snor.quotaguard.domain.enums.PenaltyType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PenaltyEventResponse(
        UUID id,
        UUID userId,
        PenaltyType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean active
) {
}

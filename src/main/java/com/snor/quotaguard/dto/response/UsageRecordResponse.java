package com.snor.quotaguard.dto.response;

import com.snor.quotaguard.domain.enums.ActionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsageRecordResponse(
        UUID id,
        UUID userId,
        int amountConsumed,
        ActionType actionType,
        LocalDateTime timestamp
) {
}

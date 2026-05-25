package com.snor.quotaguard.exception;

import com.snor.quotaguard.domain.enums.PenaltyType;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ActivePenaltyException extends RuntimeException {
    private final PenaltyType penaltyType;
    private final LocalDateTime endsAt;

    public ActivePenaltyException(PenaltyType penaltyType, LocalDateTime endsAt) {
        super("Consumption is blocked by active penalty " + penaltyType + " until " + endsAt);
        this.penaltyType = penaltyType;
        this.endsAt = endsAt;
    }
}

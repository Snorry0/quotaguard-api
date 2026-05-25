package com.snor.quotaguard.exception;

import com.snor.quotaguard.domain.enums.PenaltyType;

public class QuotaExceededException extends RuntimeException {
    private final int dailyLimit;
    private final int usedToday;
    private final int attemptedAmount;
    private final PenaltyType penaltyType;

    public QuotaExceededException(int dailyLimit, int usedToday, int attemptedAmount, PenaltyType penaltyType) {
        super("Daily quota exceeded. Limit=" + dailyLimit + ", used=" + usedToday + ", attempted=" + attemptedAmount);
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
        this.attemptedAmount = attemptedAmount;
        this.penaltyType = penaltyType;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public int getUsedToday() {
        return usedToday;
    }

    public int getAttemptedAmount() {
        return attemptedAmount;
    }

    public PenaltyType getPenaltyType() {
        return penaltyType;
    }
}

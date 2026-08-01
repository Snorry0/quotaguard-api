package com.snor.quotaguard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_quotas",
        indexes = @Index(name = "idx_user_quota_user", columnList = "user_id", unique = true)
)
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int dailyLimit;

    @Column(nullable = false)
    private int usedToday;

    @Column(nullable = false)
    private LocalDate lastResetDate;

    @Column(nullable = false)
    private int penaltyLevel;

    @Version
    private long version;

    public boolean canConsume(int amount) {
        return usedToday + amount <= dailyLimit;
    }

    public int remainingToday() {
        return Math.max(0, dailyLimit - usedToday);
    }

    public void consume(int amount) {
        if (!canConsume(amount)) {
            throw new IllegalStateException("Consumption would exceed the daily limit");
        }
        this.usedToday += amount;
    }

    public void incrementPenaltyLevel() {
        this.penaltyLevel++;
    }

    public void resetForNewDay(LocalDate resetDate, int penaltyDecay) {
        this.usedToday = 0;
        this.lastResetDate = resetDate;
        this.penaltyLevel = Math.max(0, this.penaltyLevel - penaltyDecay);
    }
}

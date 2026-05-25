package com.snor.quotaguard.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.PenaltyType;
import com.snor.quotaguard.dto.response.PenaltyEventResponse;
import com.snor.quotaguard.mapper.PenaltyEventMapper;
import com.snor.quotaguard.repository.PenaltyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyEventRepository penaltyEventRepository;
    private final PenaltyEventMapper penaltyEventMapper;
    private final QuotaGuardProperties properties;
    private final Clock clock;

    @Transactional
    public PenaltyEvent applyQuotaViolation(User user, UserQuota quota) {
        int newLevel = quota.getPenaltyLevel() + 1;
        quota.setPenaltyLevel(newLevel);

        LocalDateTime now = LocalDateTime.now(clock);
        PenaltyType type = resolvePenaltyType(newLevel);
        LocalDateTime endTime = resolveEndTime(now, type);
        boolean active = type != PenaltyType.WARNING;

        return penaltyEventRepository.save(PenaltyEvent.builder()
                .user(user)
                .type(type)
                .startTime(now)
                .endTime(endTime)
                .active(active)
                .build());
    }

    @Transactional(readOnly = true)
    public Optional<PenaltyEvent> findActiveBlockingPenalty(User user) {
        return penaltyEventRepository.findFirstByUserAndActiveTrueAndEndTimeAfterOrderByEndTimeDesc(
                user,
                LocalDateTime.now(clock)
        );
    }

    @Transactional(readOnly = true)
    public List<PenaltyEventResponse> getPenaltyHistory(User user) {
        return penaltyEventRepository.findByUserOrderByStartTimeDesc(user).stream()
                .map(penaltyEventMapper::toResponse)
                .toList();
    }

    @Transactional
    public int expireFinishedPenalties() {
        List<PenaltyEvent> expired = penaltyEventRepository.findByActiveTrueAndEndTimeBefore(LocalDateTime.now(clock));
        expired.forEach(event -> event.setActive(false));
        penaltyEventRepository.saveAll(expired);
        return expired.size();
    }

    public PenaltyType resolvePenaltyType(int penaltyLevel) {
        if (penaltyLevel <= 1) {
            return PenaltyType.WARNING;
        }
        if (penaltyLevel == 2) {
            return PenaltyType.SHORT_COOLDOWN;
        }
        return PenaltyType.LONG_COOLDOWN;
    }

    private LocalDateTime resolveEndTime(LocalDateTime startTime, PenaltyType type) {
        return switch (type) {
            case WARNING -> startTime;
            case SHORT_COOLDOWN -> startTime.plus(properties.penalties().shortCooldown());
            case LONG_COOLDOWN -> startTime.plus(properties.penalties().longCooldown());
        };
    }
}

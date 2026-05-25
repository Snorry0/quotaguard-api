package com.snor.quotaguard.service;

import com.snor.quotaguard.config.QuotaGuardProperties;
import com.snor.quotaguard.domain.UsageSession;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.domain.enums.ActionType;
import com.snor.quotaguard.domain.enums.SessionStatus;
import com.snor.quotaguard.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.dto.request.EndUsageSessionRequest;
import com.snor.quotaguard.dto.request.StartUsageSessionRequest;
import com.snor.quotaguard.dto.response.ConsumeUsageResponse;
import com.snor.quotaguard.dto.response.EndUsageSessionResponse;
import com.snor.quotaguard.dto.response.UsageSessionResponse;
import com.snor.quotaguard.exception.ActivePenaltyException;
import com.snor.quotaguard.exception.ActiveSessionAlreadyExistsException;
import com.snor.quotaguard.exception.InvalidSessionStateException;
import com.snor.quotaguard.exception.QuotaExceededException;
import com.snor.quotaguard.exception.ResourceNotFoundException;
import com.snor.quotaguard.mapper.UsageSessionMapper;
import com.snor.quotaguard.repository.UsageSessionRepository;
import com.snor.quotaguard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsageSessionService {

    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    private final UsageSessionRepository usageSessionRepository;
    private final UsageSessionMapper usageSessionMapper;
    private final CurrentUserProvider currentUserProvider;
    private final QuotaService quotaService;
    private final PenaltyService penaltyService;
    private final UsageService usageService;
    private final QuotaGuardProperties properties;
    private final Clock clock;

    @Transactional
    public UsageSessionResponse startSession(StartUsageSessionRequest request) {
        User user = currentUserProvider.getCurrentUser();

        UserQuota quota = quotaService.getQuotaForUpdate(user);
        quotaService.resetIfNewDay(quota);
        penaltyService.expireFinishedPenalties();

        penaltyService.findActiveBlockingPenalty(user)
                .ifPresent(activePenalty -> {
                    throw new ActivePenaltyException(
                            activePenalty.getType(),
                            activePenalty.getEndTime()
                    );
                });

        usageSessionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(user, SessionStatus.ACTIVE)
                .ifPresent(activeSession -> {
                    throw new ActiveSessionAlreadyExistsException(activeSession.getId());
                });

        UsageSession session = UsageSession.builder()
                .user(user)
                .clientReference(request.clientReference())
                .metadata(request.metadata())
                .startedAt(LocalDateTime.now(clock))
                .status(SessionStatus.ACTIVE)
                .build();

        return usageSessionMapper.toResponse(usageSessionRepository.save(session));
    }

    @Transactional(noRollbackFor = {
            QuotaExceededException.class,
            ActivePenaltyException.class
    })
    public EndUsageSessionResponse endSession(UUID sessionId, EndUsageSessionRequest request) {
        User user = currentUserProvider.getCurrentUser();

        UsageSession session = usageSessionRepository.findByIdAndUserIdForUpdate(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usage session not found"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidSessionStateException("Only active sessions can be ended");
        }

        LocalDateTime endedAt = LocalDateTime.now(clock);
        long durationSeconds = Math.max(
                0,
                Duration.between(session.getStartedAt(), endedAt).toSeconds()
        );

        int amountConsumed = request.amountConsumed() != null
                ? request.amountConsumed()
                : calculateConsumption(durationSeconds);

        session.setEndedAt(endedAt);
        session.setDurationSeconds(durationSeconds);
        session.setAmountConsumed(amountConsumed);
        session.setStatus(SessionStatus.COMPLETED);

        if (request.metadata() != null && !request.metadata().isBlank()) {
            session.setMetadata(request.metadata());
        }

        UsageSession savedSession = usageSessionRepository.save(session);

        ConsumeUsageResponse consumption = usageService.consumeForUser(
                user,
                new ConsumeUsageRequest(amountConsumed, ActionType.SESSION_ACTION)
        );

        return new EndUsageSessionResponse(
                usageSessionMapper.toResponse(savedSession),
                consumption
        );
    }

    @Transactional(readOnly = true)
    public Optional<UsageSessionResponse> getActiveSession() {
        User user = currentUserProvider.getCurrentUser();

        return usageSessionRepository
                .findFirstByUserAndStatusOrderByStartedAtDesc(user, SessionStatus.ACTIVE)
                .map(usageSessionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UsageSessionResponse> getHistory(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_HISTORY_PAGE_SIZE);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                safeSize,
                Sort.by(Sort.Direction.DESC, "startedAt")
        );

        return usageSessionRepository
                .findByUser(currentUserProvider.getCurrentUser(), pageable)
                .map(usageSessionMapper::toResponse);
    }

    private int calculateConsumption(long durationSeconds) {
        if (durationSeconds <= 0) {
            return properties.sessions().minimumCharge();
        }

        double minutes = durationSeconds / 60.0;
        int calculated = (int) Math.ceil(minutes * properties.sessions().unitsPerMinute());

        return Math.max(calculated, properties.sessions().minimumCharge());
    }
}
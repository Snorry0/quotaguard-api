package com.snor.quotaguard.repository;

import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenaltyEventRepository extends JpaRepository<PenaltyEvent, UUID> {
    Optional<PenaltyEvent> findFirstByUserAndActiveTrueAndEndTimeAfterOrderByEndTimeDesc(
            User user,
            LocalDateTime now
    );

    List<PenaltyEvent> findByUserOrderByStartTimeDesc(User user);

    List<PenaltyEvent> findByActiveTrueAndEndTimeBefore(LocalDateTime now);

    long countByUserAndTypeInAndStartTimeBetween(
            User user,
            Collection<PenaltyType> types,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}

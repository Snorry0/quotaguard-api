package com.snor.quotaguard.repository;

import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    Page<UsageRecord> findByUser(User user, Pageable pageable);

    List<UsageRecord> findByUserAndTimestampBetweenOrderByTimestampAsc(
            User user,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}

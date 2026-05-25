package com.snor.quotaguard.repository;

import com.snor.quotaguard.domain.UsageSession;
import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsageSessionRepository extends JpaRepository<UsageSession, UUID> {

    Optional<UsageSession> findFirstByUserAndStatusOrderByStartedAtDesc(
            User user,
            SessionStatus status
    );

    Page<UsageSession> findByUser(User user, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select s
           from UsageSession s
           where s.id = :sessionId
             and s.user.id = :userId
           """)
    Optional<UsageSession> findByIdAndUserIdForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId
    );
}
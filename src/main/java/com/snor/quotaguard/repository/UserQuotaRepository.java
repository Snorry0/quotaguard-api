package com.snor.quotaguard.repository;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.domain.UserQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserQuotaRepository extends JpaRepository<UserQuota, UUID> {
    Optional<UserQuota> findByUser(User user);
    Optional<UserQuota> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from UserQuota q where q.user.id = :userId")
    Optional<UserQuota> findByUserIdForUpdate(@Param("userId") UUID userId);
}

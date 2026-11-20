
package com.ravikanth.releasemanagement.release;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    Page<Release> findByEnvironmentId(
            Long environmentId,
            Pageable pageable
    );

    Optional<Release> findByEnvironmentIdAndVersionIgnoreCase(
            Long environmentId,
            String version
    );

    boolean existsByEnvironmentIdAndVersionIgnoreCase(
            Long environmentId,
            String version
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Release r WHERE r.id = :releaseId")
    Optional<Release> findByIdForUpdate(
            @Param("releaseId") Long releaseId
    );
}

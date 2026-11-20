
package com.ravikanth.releasemanagement.changerequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository
        extends JpaRepository<ChangeRequest, Long> {

    Page<ChangeRequest> findByEnvironmentId(
            Long environmentId,
            Pageable pageable
    );

    Page<ChangeRequest> findByRequestedById(
            Long userId,
            Pageable pageable
    );

    List<ChangeRequest> findByStatus(ChangeRequestStatus status);
}

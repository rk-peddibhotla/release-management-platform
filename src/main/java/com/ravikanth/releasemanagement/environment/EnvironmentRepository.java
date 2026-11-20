
package com.ravikanth.releasemanagement.environment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository
        extends JpaRepository<Environment, Long> {

    List<Environment> findByApplicationId(Long applicationId);

    Page<Environment> findByApplicationId(
            Long applicationId,
            Pageable pageable
    );

    Optional<Environment> findByApplicationIdAndNameIgnoreCase(
            Long applicationId,
            String name
    );

    boolean existsByApplicationIdAndNameIgnoreCase(
            Long applicationId,
            String name
    );
}

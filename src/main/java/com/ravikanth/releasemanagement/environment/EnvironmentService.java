
package com.ravikanth.releasemanagement.environment;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.application.ApplicationNotFoundException;
import com.ravikanth.releasemanagement.application.ApplicationRepository;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditLogService auditLogService;

    public EnvironmentService(
            EnvironmentRepository environmentRepository,
            ApplicationRepository applicationRepository,
            AuditLogService auditLogService
    ) {
        this.environmentRepository = environmentRepository;
        this.applicationRepository = applicationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Environment createEnvironment(
            Long applicationId,
            String name,
            EnvironmentType environmentType,
            Long performedById
    ) {
        validateApplicationId(applicationId);
        validateName(name);
        validateEnvironmentType(environmentType);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(applicationId)
                );

        String normalizedName = name.trim();

        if (environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        applicationId,
                        normalizedName
                )) {
            throw new EnvironmentAlreadyExistsException(
                    normalizedName,
                    applicationId
            );
        }

        Environment environment = new Environment(
                application,
                normalizedName,
                environmentType
        );

        Environment savedEnvironment =
                environmentRepository.save(environment);

        auditLogService.recordAudit(
                performedById,
                "ENVIRONMENT",
                savedEnvironment.getId(),
                "ENVIRONMENT_CREATED",
                null
        );

        return savedEnvironment;
    }

    @Transactional(readOnly = true)
    public Environment getEnvironmentById(Long id) {
        validateEnvironmentId(id);

        return environmentRepository.findById(id)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(id)
                );
    }

    @Transactional(readOnly = true)
    public Page<Environment> getEnvironmentsByApplication(
            Long applicationId,
            Pageable pageable
    ) {
        validateApplicationId(applicationId);

        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        return environmentRepository.findByApplicationId(
                applicationId,
                pageable
        );
    }

    @Transactional
    public Environment updateEnvironment(
            Long id,
            String name,
            EnvironmentType environmentType,
            Long performedById
    ) {
        validateEnvironmentId(id);
        validateName(name);
        validateEnvironmentType(environmentType);

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(id)
                );

        String normalizedName = name.trim();

        Long applicationId = environment.getApplication().getId();

        if (environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        applicationId,
                        normalizedName
                )
                && !environment.getName().equalsIgnoreCase(normalizedName)) {

            throw new EnvironmentAlreadyExistsException(
                    normalizedName,
                    applicationId
            );
        }

        environment.updateDetails(
                normalizedName,
                environmentType
        );

        auditLogService.recordAudit(
                performedById,
                "ENVIRONMENT",
                environment.getId(),
                "ENVIRONMENT_UPDATED",
                null
        );

        return environment;
    }

    @Transactional
    public Environment deactivateEnvironment(
            Long id,
            Long performedById
    ) {
        validateEnvironmentId(id);

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(id)
                );

        environment.deactivate();

        auditLogService.recordAudit(
                performedById,
                "ENVIRONMENT",
                environment.getId(),
                "ENVIRONMENT_DEACTIVATED",
                null
        );

        return environment;
    }

    @Transactional
    public Environment activateEnvironment(
            Long id,
            Long performedById
    ) {
        validateEnvironmentId(id);

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(id)
                );

        environment.activate();

        auditLogService.recordAudit(
                performedById,
                "ENVIRONMENT",
                environment.getId(),
                "ENVIRONMENT_ACTIVATED",
                null
        );

        return environment;
    }

    private void validateApplicationId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Application ID must be positive."
            );
        }
    }

    private void validateEnvironmentId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Environment ID must be positive."
            );
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Environment name cannot be blank."
            );
        }
    }

    private void validateEnvironmentType(
            EnvironmentType environmentType
    ) {
        if (environmentType == null) {
            throw new IllegalArgumentException(
                    "Environment type cannot be null."
            );
        }
    }
}

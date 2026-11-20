package com.ravikanth.releasemanagement.application;

import com.ravikanth.releasemanagement.common.audit.AuditLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final AuditLogService auditLogService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            AuditLogService auditLogService
    ) {
        this.applicationRepository = applicationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Application createApplication(
            String name,
            String description,
            Long performedById
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Application name cannot be blank."
            );
        }

        String normalizedName = name.trim();

        if (applicationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ApplicationAlreadyExistsException(normalizedName);
        }

        Application application = new Application(
                normalizedName,
                normalizeDescription(description)
        );

        Application savedApplication =
                applicationRepository.save(application);

        auditLogService.recordAudit(
                performedById,
                "APPLICATION",
                savedApplication.getId(),
                "APPLICATION_CREATED",
                null
        );

        return savedApplication;
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(Long id) {
        validateApplicationId(id);

        return applicationRepository.findById(id)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(id)
                );
    }

    @Transactional(readOnly = true)
    public Application getApplicationByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Application name cannot be blank."
            );
        }

        String normalizedName = name.trim();

        return applicationRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(normalizedName)
                );
    }

    @Transactional(readOnly = true)
    public Page<Application> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    @Transactional
    public Application updateApplication(
            Long id,
            String name,
            String description,
            Long performedById
    ) {
        validateApplicationId(id);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Application name cannot be blank."
            );
        }

        String normalizedName = name.trim();

        Application application = applicationRepository.findById(id)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(id)
                );

        if (applicationRepository.existsByNameIgnoreCase(normalizedName)
                && !application.getName().equalsIgnoreCase(normalizedName)) {
            throw new ApplicationAlreadyExistsException(normalizedName);
        }

        application.updateDetails(
                normalizedName,
                normalizeDescription(description)
        );

        auditLogService.recordAudit(
                performedById,
                "APPLICATION",
                application.getId(),
                "APPLICATION_UPDATED",
                null
        );

        return application;
    }

    @Transactional
    public Application deactivateApplication(
            Long id,
            Long performedById
    ) {
        validateApplicationId(id);

        Application application = applicationRepository.findById(id)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(id)
                );

        application.deactivate();

        auditLogService.recordAudit(
                performedById,
                "APPLICATION",
                application.getId(),
                "APPLICATION_DEACTIVATED",
                null
        );

        return application;
    }

    @Transactional
    public Application activateApplication(
            Long id,
            Long performedById
    ) {
        validateApplicationId(id);

        Application application = applicationRepository.findById(id)
                .orElseThrow(
                        () -> new ApplicationNotFoundException(id)
                );

        application.activate();

        auditLogService.recordAudit(
                performedById,
                "APPLICATION",
                application.getId(),
                "APPLICATION_ACTIVATED",
                null
        );

        return application;
    }

    private void validateApplicationId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Application ID must be positive."
            );
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
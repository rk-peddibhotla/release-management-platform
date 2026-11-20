
package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestNotFoundException;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestRepository;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestStatus;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException;
import com.ravikanth.releasemanagement.environment.EnvironmentRepository;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final EnvironmentRepository environmentRepository;
    private final UserRepository userRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final AuditLogService auditLogService;

    public ReleaseService(
            ReleaseRepository releaseRepository,
            EnvironmentRepository environmentRepository,
            UserRepository userRepository,
            ChangeRequestRepository changeRequestRepository,
            AuditLogService auditLogService
    ) {
        this.releaseRepository = releaseRepository;
        this.environmentRepository = environmentRepository;
        this.userRepository = userRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Release createRelease(
            Long environmentId,
            String version,
            String description,
            OffsetDateTime scheduledAt,
            Long createdById
    ) {
        validateEnvironmentId(environmentId);
        validateUserId(createdById);
        validateVersion(version);

        Environment environment = environmentRepository.findById(environmentId)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(environmentId)
                );

        User createdBy = userRepository.findById(createdById)
                .orElseThrow(
                        () -> new UserNotFoundException(createdById)
                );

        String normalizedVersion = version.trim();

        if (releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                environmentId,
                normalizedVersion
        )) {
            throw new ReleaseAlreadyExistsException(
                    normalizedVersion,
                    environmentId
            );
        }

        Release release = new Release(
                environment,
                normalizedVersion,
                normalizeDescription(description),
                scheduledAt,
                createdBy
        );

        Release savedRelease = releaseRepository.save(release);

        auditLogService.recordAudit(
                createdById,
                "RELEASE",
                savedRelease.getId(),
                "RELEASE_CREATED",
                null
        );

        return savedRelease;
    }

    @Transactional(readOnly = true)
    public Release getReleaseById(Long id) {
        validateReleaseId(id);

        return releaseRepository.findById(id)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(id)
                );
    }

    @Transactional(readOnly = true)
    public Page<Release> getReleasesByEnvironment(
            Long environmentId,
            Pageable pageable
    ) {
        validateEnvironmentId(environmentId);

        if (!environmentRepository.existsById(environmentId)) {
            throw new EnvironmentNotFoundException(environmentId);
        }

        return releaseRepository.findByEnvironmentId(
                environmentId,
                pageable
        );
    }

    @Transactional
    public Release updateRelease(
            Long id,
            String version,
            String description,
            OffsetDateTime scheduledAt,
            Long performedById
    ) {
        validateReleaseId(id);
        validateVersion(version);
        validateUserId(performedById);

        Release release = releaseRepository.findById(id)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(id)
                );

        if (release.getStatus() != ReleaseStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only scheduled releases can be updated."
            );
        }

        String normalizedVersion = version.trim();
        Long environmentId = release.getEnvironment().getId();

        if (releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                environmentId,
                normalizedVersion
        ) && !release.getVersion().equalsIgnoreCase(normalizedVersion)) {

            throw new ReleaseAlreadyExistsException(
                    normalizedVersion,
                    environmentId
            );
        }

        release.updateDetails(
                normalizedVersion,
                normalizeDescription(description),
                scheduledAt
        );

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "RELEASE_UPDATED",
                null
        );

        return release;
    }

    @Transactional
    public Release cancelRelease(
            Long id,
            Long performedById
    ) {
        validateReleaseId(id);
        validateUserId(performedById);

        Release release = releaseRepository.findById(id)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(id)
                );

        release.cancel();

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "RELEASE_CANCELLED",
                null
        );

        return release;
    }

    @Transactional
    public Release addChangeRequest(
            Long releaseId,
            Long changeRequestId,
            Long performedById
    ) {
        validateReleaseId(releaseId);
        validateChangeRequestId(changeRequestId);
        validateUserId(performedById);

        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(releaseId)
                );

        ChangeRequest changeRequest =
                changeRequestRepository.findById(changeRequestId)
                        .orElseThrow(
                                () -> new ChangeRequestNotFoundException(
                                        changeRequestId
                                )
                        );

        if (release.getStatus() != ReleaseStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Change requests can only be added to scheduled releases."
            );
        }

        if (changeRequest.getStatus() != ChangeRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved change requests can be added to a release."
            );
        }

        if (changeRequest.getRelease() != null) {
            throw new IllegalStateException(
                    "This change request is already assigned to a release."
            );
        }

        if (!release.getEnvironment().getId().equals(
                changeRequest.getEnvironment().getId()
        )) {
            throw new IllegalArgumentException(
                    "The release and change request must belong to the same environment."
            );
        }

        changeRequest.assignToRelease(release);

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "CHANGE_REQUEST_ADDED_TO_RELEASE",
                "Change request ID: " + changeRequestId
        );

        return release;
    }

    @Transactional
    public Release removeChangeRequest(
            Long releaseId,
            Long changeRequestId,
            Long performedById
    ) {
        validateReleaseId(releaseId);
        validateChangeRequestId(changeRequestId);
        validateUserId(performedById);

        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(releaseId)
                );

        ChangeRequest changeRequest =
                changeRequestRepository.findById(changeRequestId)
                        .orElseThrow(
                                () -> new ChangeRequestNotFoundException(
                                        changeRequestId
                                )
                        );

        if (release.getStatus() != ReleaseStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Change requests can only be removed from scheduled releases."
            );
        }

        if (changeRequest.getRelease() == null
                || !changeRequest.getRelease().getId().equals(releaseId)) {

            throw new IllegalStateException(
                    "This change request is not assigned to this release."
            );
        }

        changeRequest.removeFromRelease();

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "CHANGE_REQUEST_REMOVED_FROM_RELEASE",
                "Change request ID: " + changeRequestId
        );

        return release;
    }

    @Transactional
    public Release startDeployment(
            Long id,
            Long performedById
    ) {
        validateReleaseId(id);
        validateUserId(performedById);

        Release release = releaseRepository.findById(id)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(id)
                );

        release.start();

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "RELEASE_STARTED",
                null
        );

        return release;
    }

    @Transactional
    public Release completeRelease(
            Long id,
            Long performedById
    ) {
        validateReleaseId(id);
        validateUserId(performedById);

        Release release = releaseRepository.findById(id)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(id)
                );

        release.complete();

        auditLogService.recordAudit(
                performedById,
                "RELEASE",
                release.getId(),
                "RELEASE_COMPLETED",
                null
        );

        return release;
    }

    private void validateReleaseId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Release ID must be positive."
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

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be positive."
            );
        }
    }

    private void validateChangeRequestId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Change request ID must be positive."
            );
        }
    }

    private void validateVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Release version cannot be blank."
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

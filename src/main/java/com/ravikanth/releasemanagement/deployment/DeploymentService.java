
package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.release.ReleaseNotFoundException;
import com.ravikanth.releasemanagement.release.ReleaseRepository;
import com.ravikanth.releasemanagement.release.ReleaseStatus;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ReleaseRepository releaseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DeploymentService(
            DeploymentRepository deploymentRepository,
            ReleaseRepository releaseRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.deploymentRepository = deploymentRepository;
        this.releaseRepository = releaseRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Deployment recordDeployment(
            Long releaseId,
            Long deployedById,
            DeploymentStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String notes
    ) {
        validateReleaseId(releaseId);
        validateUserId(deployedById);
        validateStatus(status);
        validateDeploymentTimes(startedAt, completedAt);

        Release release = releaseRepository.findByIdForUpdate(releaseId)
                .orElseThrow(
                        () -> new ReleaseNotFoundException(releaseId)
                );

        if (release.getStatus() != ReleaseStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Deployment attempts can only be recorded for releases in progress."
            );
        }

        User deployedBy = userRepository.findById(deployedById)
                .orElseThrow(
                        () -> new UserNotFoundException(deployedById)
                );

        if (!deployedBy.isActive()) {
            throw new IllegalStateException(
                    "An inactive user cannot record a deployment."
            );
        }

        int attemptNumber = deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(releaseId)
                .map(deployment -> deployment.getAttemptNumber() + 1)
                .orElse(1);

        Deployment deployment = new Deployment(
                release,
                attemptNumber,
                status,
                deployedBy,
                startedAt,
                completedAt,
                normalizeNotes(notes)
        );

        Deployment savedDeployment = deploymentRepository.save(deployment);

        String auditAction = status == DeploymentStatus.SUCCESS
                ? "DEPLOYMENT_SUCCEEDED"
                : "DEPLOYMENT_FAILED";

        String auditDetails =
                "Deployment attempt: " + attemptNumber
                + ", status: " + status;

        auditLogService.recordAudit(
                deployedById,
                "RELEASE",
                releaseId,
                auditAction,
                auditDetails
        );

        if (status == DeploymentStatus.SUCCESS) {
            release.complete();

            auditLogService.recordAudit(
                    deployedById,
                    "RELEASE",
                    releaseId,
                    "RELEASE_COMPLETED",
                    "Completed after successful deployment attempt: "
                            + attemptNumber
            );
        }

        return savedDeployment;
    }

    @Transactional(readOnly = true)
    public List<Deployment> getDeploymentsByReleaseId(Long releaseId) {
        validateReleaseId(releaseId);

        if (!releaseRepository.existsById(releaseId)) {
            throw new ReleaseNotFoundException(releaseId);
        }

        return deploymentRepository
                .findByReleaseIdOrderByAttemptNumberAsc(releaseId);
    }

    @Transactional(readOnly = true)
    public Deployment getDeploymentById(Long id) {
        validateDeploymentId(id);

        return deploymentRepository.findById(id)
                .orElseThrow(
                        () -> new DeploymentNotFoundException(id)
                );
    }

    private void validateReleaseId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Release ID must be positive."
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

    private void validateDeploymentId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Deployment ID must be positive."
            );
        }
    }

    private void validateStatus(DeploymentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Deployment status cannot be null."
            );
        }
    }

    private void validateDeploymentTimes(
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
        if (startedAt == null) {
            throw new IllegalArgumentException(
                    "Deployment start time cannot be null."
            );
        }

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "Deployment completion time cannot be null."
            );
        }

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Deployment completion time cannot be before its start time."
            );
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }

        return notes.trim();
    }
}

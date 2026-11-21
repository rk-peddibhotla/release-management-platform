package com.ravikanth.releasemanagement.deployment.dto;

import com.ravikanth.releasemanagement.deployment.Deployment;
import com.ravikanth.releasemanagement.deployment.DeploymentStatus;

import java.time.OffsetDateTime;

public record DeploymentResponse(
        Long id,
        Long releaseId,
        int attemptNumber,
        DeploymentStatus status,
        Long deployedById,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String notes
) {
    public static DeploymentResponse from(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getRelease().getId(),
                deployment.getAttemptNumber(),
                deployment.getStatus(),
                deployment.getDeployedBy().getId(),
                deployment.getStartedAt(),
                deployment.getCompletedAt(),
                deployment.getNotes()
        );
    }
}

package com.ravikanth.releasemanagement.deployment.dto;

import com.ravikanth.releasemanagement.deployment.DeploymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record DeploymentRequest(
        @NotNull(message = "Deployment status is required.")
        DeploymentStatus status,

        @NotNull(message = "Deployment start time is required.")
        OffsetDateTime startedAt,

        @NotNull(message = "Deployment completion time is required.")
        OffsetDateTime completedAt,

        @Size(max = 2000, message = "Deployment notes must not exceed 2000 characters.")
        String notes
) {}

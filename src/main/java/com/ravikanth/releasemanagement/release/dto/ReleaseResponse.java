package com.ravikanth.releasemanagement.release.dto;

import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.release.ReleaseStatus;

import java.time.OffsetDateTime;

public record ReleaseResponse(
        Long id,
        Long environmentId,
        String version,
        String description,
        ReleaseStatus status,
        OffsetDateTime scheduledAt,
        Long createdById,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                release.getEnvironment().getId(),
                release.getVersion(),
                release.getDescription(),
                release.getStatus(),
                release.getScheduledAt(),
                release.getCreatedBy().getId(),
                release.getCreatedAt(),
                release.getUpdatedAt()
        );
    }
}
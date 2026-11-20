package com.ravikanth.releasemanagement.changerequest.dto;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestStatus;
import com.ravikanth.releasemanagement.changerequest.ChangeType;

import java.time.OffsetDateTime;

public record ChangeRequestResponse(
        Long id,
        Long environmentId,
        Long releaseId,
        String title,
        String description,
        ChangeType changeType,
        ChangeRequestStatus status,
        Long requestedById,
        OffsetDateTime plannedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ChangeRequestResponse from(ChangeRequest changeRequest) {
        return new ChangeRequestResponse(
                changeRequest.getId(),
                changeRequest.getEnvironment().getId(),
                changeRequest.getRelease() != null
                        ? changeRequest.getRelease().getId()
                        : null,
                changeRequest.getTitle(),
                changeRequest.getDescription(),
                changeRequest.getChangeType(),
                changeRequest.getStatus(),
                changeRequest.getRequestedBy().getId(),
                changeRequest.getPlannedAt(),
                changeRequest.getCreatedAt(),
                changeRequest.getUpdatedAt()
        );
    }
}

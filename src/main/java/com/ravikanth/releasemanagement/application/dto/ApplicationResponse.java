package com.ravikanth.releasemanagement.application.dto;

import com.ravikanth.releasemanagement.application.Application;

import java.time.OffsetDateTime;

public record ApplicationResponse(
        Long id,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getName(),
                application.getDescription(),
                application.isActive(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
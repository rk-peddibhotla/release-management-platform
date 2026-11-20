
package com.ravikanth.releasemanagement.environment.dto;

import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentType;

import java.time.OffsetDateTime;

public record EnvironmentResponse(
        Long id,
        Long applicationId,
        String applicationName,
        String name,
        EnvironmentType environmentType,
        boolean active,
        OffsetDateTime createdAt
) {
    public static EnvironmentResponse from(Environment environment) {
        return new EnvironmentResponse(
                environment.getId(),
                environment.getApplication().getId(),
                environment.getApplication().getName(),
                environment.getName(),
                environment.getEnvironmentType(),
                environment.isActive(),
                environment.getCreatedAt()
        );
    }
}

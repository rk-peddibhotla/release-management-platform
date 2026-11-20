
package com.ravikanth.releasemanagement.user.dto;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

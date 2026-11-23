package com.ravikanth.releasemanagement.common.audit;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String userName,
        String entityType,
        Long entityId,
        String action,
        String details,
        OffsetDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUser().getId(),
                auditLog.getUser().getName(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
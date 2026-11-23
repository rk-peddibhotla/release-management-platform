package com.ravikanth.releasemanagement.common.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'APPROVER')")
@Tag(
        name = "Audit Logs",
        description = "Review recorded workflow activity."
)
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity audit history")
    public ResponseEntity<Page<AuditLogResponse>> getAuditHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<AuditLogResponse> history = auditLogService
                .getAuditHistory(entityType, entityId, pageable)
                .map(AuditLogResponse::from);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user audit history")
    public ResponseEntity<Page<AuditLogResponse>> getAuditHistoryByUser(
            @PathVariable Long userId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<AuditLogResponse> history = auditLogService
                .getAuditHistoryByUser(userId, pageable)
                .map(AuditLogResponse::from);

        return ResponseEntity.ok(history);
    }
}
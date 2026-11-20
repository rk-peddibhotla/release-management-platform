
package com.ravikanth.releasemanagement.common.audit;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuditLog recordAudit(
            Long userId,
            String entityType,
            Long entityId,
            String action,
            String details
    ) {
        validateUserId(userId);
        validateEntityType(entityType);
        validateEntityId(entityId);
        validateAction(action);

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId)
                );

        AuditLog auditLog = new AuditLog(
                user,
                entityType.trim().toUpperCase(),
                entityId,
                action.trim().toUpperCase(),
                normalizeDetails(details)
        );

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditHistory(
            String entityType,
            Long entityId,
            Pageable pageable
    ) {
        validateEntityType(entityType);
        validateEntityId(entityId);

        return auditLogRepository.findByEntityTypeAndEntityId(
                entityType.trim().toUpperCase(),
                entityId,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditHistoryByUser(
            Long userId,
            Pageable pageable
    ) {
        validateUserId(userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return auditLogRepository.findByUserId(
                userId,
                pageable
        );
    }

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be positive."
            );
        }
    }

    private void validateEntityId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Entity ID must be positive."
            );
        }
    }

    private void validateEntityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException(
                    "Entity type cannot be blank."
            );
        }
    }

    private void validateAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Audit action cannot be blank."
            );
        }
    }

    private String normalizeDetails(String details) {
        if (details == null || details.isBlank()) {
            return "{}";
        }

        return "{\"message\":\"" + escapeJson(details.trim()) + "\"}";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();

        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");

                default -> {
                    if (character < 0x20) {
                        escaped.append(
                                String.format("\\u%04x", (int) character)
                        );
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.toString();
    }
}

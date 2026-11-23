
package com.ravikanth.releasemanagement.changerequest;

import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException;
import com.ravikanth.releasemanagement.environment.EnvironmentRepository;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final EnvironmentRepository environmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ChangeRequestService(
            ChangeRequestRepository changeRequestRepository,
            EnvironmentRepository environmentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.changeRequestRepository = changeRequestRepository;
        this.environmentRepository = environmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ChangeRequest createChangeRequest(
            Long environmentId,
            String title,
            String description,
            ChangeType changeType,
            Long requestedById,
            OffsetDateTime plannedAt
    ) {
        validateEnvironmentId(environmentId);
        validateUserId(requestedById);
        validateTitle(title);
        validateChangeType(changeType);

        Environment environment = environmentRepository.findById(environmentId)
                .orElseThrow(
                        () -> new EnvironmentNotFoundException(environmentId)
                );

        User requestedBy = userRepository.findById(requestedById)
                .orElseThrow(
                        () -> new UserNotFoundException(requestedById)
                );

        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                title.trim(),
                normalizeDescription(description),
                changeType,
                requestedBy,
                plannedAt
        );

        return changeRequestRepository.save(changeRequest);
    }

    @Transactional(readOnly = true)
    public ChangeRequest getChangeRequestById(Long id) {
        validateChangeRequestId(id);

        return changeRequestRepository.findById(id)
                .orElseThrow(
                        () -> new ChangeRequestNotFoundException(id)
                );
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequest> getChangeRequestsByEnvironment(
            Long environmentId,
            Pageable pageable
    ) {
        validateEnvironmentId(environmentId);

        if (!environmentRepository.existsById(environmentId)) {
            throw new EnvironmentNotFoundException(environmentId);
        }

        return changeRequestRepository.findByEnvironmentId(
                environmentId,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequest> getChangeRequestsByRequester(
            Long userId,
            Pageable pageable
    ) {
        validateUserId(userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return changeRequestRepository.findByRequestedById(
                userId,
                pageable
        );
    }

    @Transactional
    public ChangeRequest updateChangeRequest(
            Long id,
            String title,
            String description,
            ChangeType changeType,
            OffsetDateTime plannedAt
    ) {
        validateChangeRequestId(id);
        validateTitle(title);
        validateChangeType(changeType);

        ChangeRequest changeRequest = changeRequestRepository.findById(id)
                .orElseThrow(
                        () -> new ChangeRequestNotFoundException(id)
                );

        if (changeRequest.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft change requests can be updated."
            );
        }

        changeRequest.updateDetails(
                title.trim(),
                normalizeDescription(description),
                changeType,
                plannedAt
        );

        return changeRequest;
    }

    @Transactional
    public ChangeRequest submitChangeRequest(
            Long id,
            Long submittedById
    ) {
        validateChangeRequestId(id);
        validateUserId(submittedById);

        ChangeRequest changeRequest = changeRequestRepository.findById(id)
                .orElseThrow(
                        () -> new ChangeRequestNotFoundException(id)
                );

        changeRequest.submit();

        auditLogService.recordAudit(
                submittedById,
                "CHANGE_REQUEST",
                changeRequest.getId(),
                "CHANGE_REQUEST_SUBMITTED",
                null
        );

        return changeRequest;
    }

    private void validateChangeRequestId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Change request ID must be positive."
            );
        }
    }

    private void validateEnvironmentId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Environment ID must be positive."
            );
        }
    }

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be positive."
            );
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Change request title cannot be blank."
            );
        }
    }

    private void validateChangeType(ChangeType changeType) {
        if (changeType == null) {
            throw new IllegalArgumentException(
                    "Change type cannot be null."
            );
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }


    @Transactional(readOnly = true)
    public Page<ChangeRequest> getPendingChangeRequests(Pageable pageable) {
        return changeRequestRepository.findByStatus(
                ChangeRequestStatus.SUBMITTED,
                pageable
        );
}











}

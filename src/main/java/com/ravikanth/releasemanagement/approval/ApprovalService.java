
package com.ravikanth.releasemanagement.approval;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestNotFoundException;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestRepository;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestStatus;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ApprovalService(
            ApprovalRepository approvalRepository,
            ChangeRequestRepository changeRequestRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.approvalRepository = approvalRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Approval approveChangeRequest(
            Long changeRequestId,
            Long approverId,
            String comments
    ) {
        return decideChangeRequest(
                changeRequestId,
                approverId,
                ApprovalDecision.APPROVED,
                comments
        );
    }

    @Transactional
    public Approval rejectChangeRequest(
            Long changeRequestId,
            Long approverId,
            String comments
    ) {
        return decideChangeRequest(
                changeRequestId,
                approverId,
                ApprovalDecision.REJECTED,
                comments
        );
    }

    private Approval decideChangeRequest(
            Long changeRequestId,
            Long approverId,
            ApprovalDecision decision,
            String comments
    ) {
        validateId(changeRequestId, "Change request ID");
        validateId(approverId, "Approver ID");

        ChangeRequest changeRequest =
                changeRequestRepository.findById(changeRequestId)
                        .orElseThrow(
                                () -> new ChangeRequestNotFoundException(
                                        changeRequestId
                                )
                        );

        if (changeRequest.getStatus()
                != ChangeRequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted change requests can receive a decision."
            );
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(
                        () -> new UserNotFoundException(approverId)
                );

        validateApprover(approver, changeRequest);

        String normalizedComments = normalizeComments(comments);

        if (decision == ApprovalDecision.APPROVED) {
            changeRequest.approve();
        } else {
            changeRequest.reject();
        }

        Approval approval = new Approval(
                changeRequest,
                approver,
                decision,
                normalizedComments
        );

        Approval savedApproval = approvalRepository.save(approval);

        auditLogService.recordAudit(
                approverId,
                "CHANGE_REQUEST",
                changeRequestId,
                decision == ApprovalDecision.APPROVED
                        ? "CHANGE_REQUEST_APPROVED"
                        : "CHANGE_REQUEST_REJECTED",
                normalizedComments
        );

        return savedApproval;
    }

    private void validateApprover(
            User approver,
            ChangeRequest changeRequest
    ) {
        if (!approver.isActive()) {
            throw new IllegalStateException(
                    "Inactive users cannot approve or reject change requests."
            );
        }

        if (approver.getRole() != UserRole.APPROVER
                && approver.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException(
                    "Only approvers and admins can decide change requests."
            );
        }

        if (changeRequest.getRequestedBy().getId()
                .equals(approver.getId())) {
            throw new IllegalStateException(
                    "Users cannot approve or reject their own change requests."
            );
        }
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive."
            );
        }
    }

    private String normalizeComments(String comments) {
        if (comments == null || comments.isBlank()) {
            return null;
        }

        return comments.trim();
    }
}

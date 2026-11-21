package com.ravikanth.releasemanagement.approval.dto;

import com.ravikanth.releasemanagement.approval.Approval;
import com.ravikanth.releasemanagement.approval.ApprovalDecision;

import java.time.OffsetDateTime;

public record ApprovalResponse(
        Long id,
        Long changeRequestId,
        Long approverId,
        ApprovalDecision decision,
        String comments,
        OffsetDateTime decidedAt
) {

    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getChangeRequest().getId(),
                approval.getApprover().getId(),
                approval.getDecision(),
                approval.getComments(),
                approval.getDecidedAt()
        );
    }
}

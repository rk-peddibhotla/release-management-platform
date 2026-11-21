package com.ravikanth.releasemanagement.approval;

import com.ravikanth.releasemanagement.approval.dto.ApprovalDecisionRequest;
import com.ravikanth.releasemanagement.approval.dto.ApprovalResponse;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'APPROVER')")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserService userService;

    public ApprovalController(
            ApprovalService approvalService,
            UserService userService
    ) {
        this.approvalService = approvalService;
        this.userService = userService;
    }

    @PostMapping("/change-requests/{changeRequestId}/approve")
    public ResponseEntity<ApprovalResponse> approveChangeRequest(
            @PathVariable Long changeRequestId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            Authentication authentication
    ) {
        Approval approval = approvalService.approveChangeRequest(
                changeRequestId,
                getUserId(authentication),
                request.comments()
        );

        return ResponseEntity.ok(
                ApprovalResponse.from(approval)
        );
    }

    @PostMapping("/change-requests/{changeRequestId}/reject")
    public ResponseEntity<ApprovalResponse> rejectChangeRequest(
            @PathVariable Long changeRequestId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            Authentication authentication
    ) {
        Approval approval = approvalService.rejectChangeRequest(
                changeRequestId,
                getUserId(authentication),
                request.comments()
        );

        return ResponseEntity.ok(
                ApprovalResponse.from(approval)
        );
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}

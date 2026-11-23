
package com.ravikanth.releasemanagement.approval;

import com.ravikanth.releasemanagement.approval.dto.ApprovalDecisionRequest;
import com.ravikanth.releasemanagement.approval.dto.ApprovalResponse;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestService;
import com.ravikanth.releasemanagement.changerequest.dto.ChangeRequestResponse;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'APPROVER')")
@Tag(name = "Approvals", description = "Review and decide change requests.")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserService userService;
    private final ChangeRequestService changeRequestService;

    public ApprovalController(
            ApprovalService approvalService,
            UserService userService,
            ChangeRequestService changeRequestService
    ) {
        this.approvalService = approvalService;
        this.userService = userService;
        this.changeRequestService = changeRequestService;
    }

    @GetMapping("/change-requests/pending")
    @Operation(summary = "List pending change requests")
    public ResponseEntity<Page<ChangeRequestResponse>> getPendingChangeRequests(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ChangeRequestResponse> requests =
                changeRequestService.getPendingChangeRequests(pageable)
                        .map(ChangeRequestResponse::from);

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/change-requests/{changeRequestId}/approve")
    @Operation(summary = "Approve change request")
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
    @Operation(summary = "Reject change request")
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

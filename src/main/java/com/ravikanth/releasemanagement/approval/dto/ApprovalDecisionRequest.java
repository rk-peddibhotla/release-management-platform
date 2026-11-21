package com.ravikanth.releasemanagement.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @Size(max = 2000, message = "Comments must not exceed 2000 characters.")
        String comments
) {}

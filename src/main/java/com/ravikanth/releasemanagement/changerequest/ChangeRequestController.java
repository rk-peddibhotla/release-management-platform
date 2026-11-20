package com.ravikanth.releasemanagement.changerequest;

import com.ravikanth.releasemanagement.changerequest.dto.ChangeRequestResponse;
import com.ravikanth.releasemanagement.changerequest.dto.CreateChangeRequestRequest;
import com.ravikanth.releasemanagement.changerequest.dto.UpdateChangeRequestRequest;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;
    private final UserService userService;

    public ChangeRequestController(
            ChangeRequestService changeRequestService,
            UserService userService
    ) {
        this.changeRequestService = changeRequestService;
        this.userService = userService;
    }

    @PostMapping("/environments/{environmentId}/change-requests")
    public ResponseEntity<ChangeRequestResponse> createChangeRequest(
            @PathVariable Long environmentId,
            @Valid @RequestBody CreateChangeRequestRequest request,
            Authentication authentication
    ) {
        ChangeRequest changeRequest = changeRequestService.createChangeRequest(
                environmentId,
                request.title(),
                request.description(),
                request.changeType(),
                getUserId(authentication),
                request.plannedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ChangeRequestResponse.from(changeRequest));
    }

    @GetMapping("/change-requests/{id}")
    public ResponseEntity<ChangeRequestResponse> getChangeRequestById(
            @PathVariable Long id
    ) {
        ChangeRequest changeRequest =
                changeRequestService.getChangeRequestById(id);

        return ResponseEntity.ok(
                ChangeRequestResponse.from(changeRequest)
        );
    }

    @GetMapping("/environments/{environmentId}/change-requests")
    public ResponseEntity<Page<ChangeRequestResponse>> getChangeRequestsByEnvironment(
            @PathVariable Long environmentId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ChangeRequestResponse> changeRequests =
                changeRequestService
                        .getChangeRequestsByEnvironment(environmentId, pageable)
                        .map(ChangeRequestResponse::from);

        return ResponseEntity.ok(changeRequests);
    }

    @GetMapping("/users/{userId}/change-requests")
    public ResponseEntity<Page<ChangeRequestResponse>> getChangeRequestsByRequester(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ChangeRequestResponse> changeRequests =
                changeRequestService
                        .getChangeRequestsByRequester(userId, pageable)
                        .map(ChangeRequestResponse::from);

        return ResponseEntity.ok(changeRequests);
    }

    @PutMapping("/change-requests/{id}")
    public ResponseEntity<ChangeRequestResponse> updateChangeRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChangeRequestRequest request
    ) {
        ChangeRequest changeRequest =
                changeRequestService.updateChangeRequest(
                        id,
                        request.title(),
                        request.description(),
                        request.changeType(),
                        request.plannedAt()
                );

        return ResponseEntity.ok(
                ChangeRequestResponse.from(changeRequest)
        );
    }

    @PatchMapping("/change-requests/{id}/submit")
    public ResponseEntity<ChangeRequestResponse> submitChangeRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ChangeRequest changeRequest =
                changeRequestService.submitChangeRequest(
                        id,
                        getUserId(authentication)
                );

        return ResponseEntity.ok(
                ChangeRequestResponse.from(changeRequest)
        );
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}

package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.release.dto.CreateReleaseRequest;
import com.ravikanth.releasemanagement.release.dto.ReleaseResponse;
import com.ravikanth.releasemanagement.release.dto.UpdateReleaseRequest;
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
public class ReleaseController {

    private final ReleaseService releaseService;
    private final UserService userService;

    public ReleaseController(
            ReleaseService releaseService,
            UserService userService
    ) {
        this.releaseService = releaseService;
        this.userService = userService;
    }

    @PostMapping("/environments/{environmentId}/releases")
    public ResponseEntity<ReleaseResponse> createRelease(
            @PathVariable Long environmentId,
            @Valid @RequestBody CreateReleaseRequest request,
            Authentication authentication
    ) {
        Release release = releaseService.createRelease(
                environmentId,
                request.version(),
                request.description(),
                request.scheduledAt(),
                getUserId(authentication)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ReleaseResponse.from(release));
    }

    @GetMapping("/releases/{id}")
    public ResponseEntity<ReleaseResponse> getReleaseById(
            @PathVariable Long id
    ) {
        Release release = releaseService.getReleaseById(id);

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    @GetMapping("/environments/{environmentId}/releases")
    public ResponseEntity<Page<ReleaseResponse>> getReleasesByEnvironment(
            @PathVariable Long environmentId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ReleaseResponse> releases =
                releaseService.getReleasesByEnvironment(
                        environmentId,
                        pageable
                ).map(ReleaseResponse::from);

        return ResponseEntity.ok(releases);
    }

    @PutMapping("/releases/{id}")
    public ResponseEntity<ReleaseResponse> updateRelease(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReleaseRequest request,
            Authentication authentication
    ) {
        Release release = releaseService.updateRelease(
                id,
                request.version(),
                request.description(),
                request.scheduledAt(),
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    @PatchMapping("/releases/{id}/cancel")
    public ResponseEntity<ReleaseResponse> cancelRelease(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Release release = releaseService.cancelRelease(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    @PostMapping("/releases/{releaseId}/change-requests/{changeRequestId}")
    public ResponseEntity<ReleaseResponse> addChangeRequest(
            @PathVariable Long releaseId,
            @PathVariable Long changeRequestId,
            Authentication authentication
    ) {
        Release release = releaseService.addChangeRequest(
                releaseId,
                changeRequestId,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    @DeleteMapping("/releases/{releaseId}/change-requests/{changeRequestId}")
    public ResponseEntity<ReleaseResponse> removeChangeRequest(
            @PathVariable Long releaseId,
            @PathVariable Long changeRequestId,
            Authentication authentication
    ) {
        Release release = releaseService.removeChangeRequest(
                releaseId,
                changeRequestId,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    @PatchMapping("/releases/{id}/start")
    public ResponseEntity<ReleaseResponse> startDeployment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Release release = releaseService.startDeployment(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                ReleaseResponse.from(release)
        );
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}
package com.ravikanth.releasemanagement.application;

import com.ravikanth.releasemanagement.application.dto.ApplicationResponse;
import com.ravikanth.releasemanagement.application.dto.CreateApplicationRequest;
import com.ravikanth.releasemanagement.application.dto.UpdateApplicationRequest;

import jakarta.validation.Valid;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    public ApplicationController(ApplicationService applicationService, UserService userService) {
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            Authentication authentication
    ) {
        Application application = applicationService.createApplication(
                request.name(),
                request.description(),
                getUserId(authentication)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.from(application));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id
    ) {
        Application application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @GetMapping
    public ResponseEntity<Page<ApplicationResponse>> getAllApplications(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ApplicationResponse> applications =
                applicationService.getAllApplications(pageable)
                        .map(ApplicationResponse::from);

        return ResponseEntity.ok(applications);
    }

    @GetMapping("/by-name")
    public ResponseEntity<ApplicationResponse> getApplicationByName(
            @RequestParam String name
    ) {
        Application application = applicationService.getApplicationByName(name);
        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request,
            Authentication authentication
    ) {
        Application application = applicationService.updateApplication(
                id,
                request.name(),
                request.description(),
                getUserId(authentication)
        );

        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApplicationResponse> activateApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Application application = applicationService.activateApplication(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApplicationResponse> deactivateApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Application application = applicationService.deactivateApplication(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}
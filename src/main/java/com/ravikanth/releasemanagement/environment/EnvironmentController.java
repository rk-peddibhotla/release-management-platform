
package com.ravikanth.releasemanagement.environment;

import com.ravikanth.releasemanagement.environment.dto.CreateEnvironmentRequest;
import com.ravikanth.releasemanagement.environment.dto.EnvironmentResponse;
import com.ravikanth.releasemanagement.environment.dto.UpdateEnvironmentRequest;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Environments", description = "Manage application environments.")
public class EnvironmentController {

    private final EnvironmentService environmentService;
    private final UserService userService;

    public EnvironmentController(
            EnvironmentService environmentService,
            UserService userService
    ) {
        this.environmentService = environmentService;
        this.userService = userService;
    }

    @PostMapping("/applications/{applicationId}/environments")
    @Operation(summary = "Create environment")
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateEnvironmentRequest request,
            Authentication authentication
    ) {
        Environment environment = environmentService.createEnvironment(
                applicationId,
                request.name(),
                request.environmentType(),
                getUserId(authentication)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EnvironmentResponse.from(environment));
    }

    @GetMapping("/environments/{id}")
    @Operation(summary = "Get environment")
    public ResponseEntity<EnvironmentResponse> getEnvironmentById(
            @PathVariable Long id
    ) {
        Environment environment =
                environmentService.getEnvironmentById(id);

        return ResponseEntity.ok(
                EnvironmentResponse.from(environment)
        );
    }

    @GetMapping("/applications/{applicationId}/environments")
    @Operation(summary = "List application environments")
    public ResponseEntity<Page<EnvironmentResponse>> getEnvironmentsByApplication(
            @PathVariable Long applicationId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<EnvironmentResponse> environments =
                environmentService.getEnvironmentsByApplication(
                        applicationId,
                        pageable
                ).map(EnvironmentResponse::from);

        return ResponseEntity.ok(environments);
    }

    @PutMapping("/environments/{id}")
    @Operation(summary = "Update environment")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnvironmentRequest request,
            Authentication authentication
    ) {
        Environment environment = environmentService.updateEnvironment(
                id,
                request.name(),
                request.environmentType(),
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                EnvironmentResponse.from(environment)
        );
    }

    @PatchMapping("/environments/{id}/activate")
    @Operation(summary = "Activate environment")
    public ResponseEntity<EnvironmentResponse> activateEnvironment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Environment environment = environmentService.activateEnvironment(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                EnvironmentResponse.from(environment)
        );
    }

    @PatchMapping("/environments/{id}/deactivate")
    @Operation(summary = "Deactivate environment")
    public ResponseEntity<EnvironmentResponse> deactivateEnvironment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Environment environment = environmentService.deactivateEnvironment(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                EnvironmentResponse.from(environment)
        );
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}

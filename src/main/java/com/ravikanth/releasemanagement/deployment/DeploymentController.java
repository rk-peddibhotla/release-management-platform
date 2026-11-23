package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.deployment.dto.DeploymentRequest;
import com.ravikanth.releasemanagement.deployment.dto.DeploymentResponse;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
@Tag(
        name = "Deployments",
        description = "Record and review deployment results."
)
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final UserService userService;

    public DeploymentController(
            DeploymentService deploymentService,
            UserService userService
    ) {
        this.deploymentService = deploymentService;
        this.userService = userService;
    }

    @PostMapping("/releases/{releaseId}/deployments")
    @Operation(summary = "Record deployment result")
    public ResponseEntity<DeploymentResponse> recordDeployment(
            @PathVariable Long releaseId,
            @Valid @RequestBody DeploymentRequest request,
            Authentication authentication
    ) {
        Deployment deployment = deploymentService.recordDeployment(
                releaseId,
                getUserId(authentication),
                request.status(),
                request.startedAt(),
                request.completedAt(),
                request.notes()
        );

        return ResponseEntity.ok(
                DeploymentResponse.from(deployment)
        );
    }

    @GetMapping("/deployments/{id}")
    @Operation(summary = "Get deployment")
    public ResponseEntity<DeploymentResponse> getDeploymentById(
            @PathVariable Long id
    ) {
        Deployment deployment = deploymentService.getDeploymentById(id);

        return ResponseEntity.ok(
                DeploymentResponse.from(deployment)
        );
    }

    @GetMapping("/releases/{releaseId}/deployments")
    @Operation(summary = "List release deployments")
    public ResponseEntity<List<DeploymentResponse>> getDeploymentsByReleaseId(
            @PathVariable Long releaseId
    ) {
        List<DeploymentResponse> deployments = deploymentService
                .getDeploymentsByReleaseId(releaseId)
                .stream()
                .map(DeploymentResponse::from)
                .toList();

        return ResponseEntity.ok(deployments);
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}
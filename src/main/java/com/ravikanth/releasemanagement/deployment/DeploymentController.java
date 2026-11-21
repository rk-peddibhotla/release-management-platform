package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.deployment.dto.DeploymentRequest;
import com.ravikanth.releasemanagement.deployment.dto.DeploymentResponse;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
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
    public ResponseEntity<DeploymentResponse> getDeploymentById(
            @PathVariable Long id
    ) {
        Deployment deployment = deploymentService.getDeploymentById(id);

        return ResponseEntity.ok(
                DeploymentResponse.from(deployment)
        );
    }

    @GetMapping("/releases/{releaseId}/deployments")
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

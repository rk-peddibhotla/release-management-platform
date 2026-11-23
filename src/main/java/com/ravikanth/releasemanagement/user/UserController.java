package com.ravikanth.releasemanagement.user;

import com.ravikanth.releasemanagement.user.dto.CreateUserRequest;
import com.ravikanth.releasemanagement.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(
        name = "Users",
        description = "Manage user accounts."
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        User user = userService.createUser(
                request.name(),
                request.email(),
                request.password(),
                request.role()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        User user = userService.getUserById(id);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping
    @Operation(summary = "List users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllUsers(pageable)
                .map(UserResponse::from);

        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate user")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Long id
    ) {
        User user = userService.activateUser(id);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Long id
    ) {
        User user = userService.deactivateUser(id);

        return ResponseEntity.ok(UserResponse.from(user));
    }
}
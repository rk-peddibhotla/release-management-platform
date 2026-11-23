
package com.ravikanth.releasemanagement.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token and account details returned after successful login.")
public class LoginResponse {

    @Schema(description = "JWT access token used to authenticate API requests.")
    private final String token;

    @Schema(
            description = "Authentication scheme to use with the access token.",
            example = "Bearer"
    )
    private final String tokenType;

    @Schema(description = "Unique ID of the authenticated user.", example = "42")
    private final Long userId;

    @Schema(description = "Display name of the authenticated user.", example = "Alex Morgan")
    private final String name;

    @Schema(description = "Email address of the authenticated user.", example = "alex@example.com")
    private final String email;

    @Schema(
            description = "Role assigned to the authenticated user.",
            example = "ENGINEER",
            allowableValues = {"ADMIN", "ENGINEER", "APPROVER"}
    )
    private final String role;

    public LoginResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role
    ) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}

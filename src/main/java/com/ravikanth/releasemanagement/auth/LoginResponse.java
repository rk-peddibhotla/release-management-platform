
package com.ravikanth.releasemanagement.auth;

public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final Long userId;
    private final String name;
    private final String email;
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

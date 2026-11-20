
package com.ravikanth.releasemanagement.common.security;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            Base64.getEncoder().encodeToString(
                    new byte[32]
            );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 86400000);
    }

    @Test
    void shouldGenerateTokenForUser() {
        User user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token))
                .isEqualTo("engineer@example.com");
    }

    @Test
    void shouldExtractEmailFromToken() {
        User user = new User(
                "Test Admin",
                "admin@example.com",
                "hashed-password",
                UserRole.ADMIN
        );

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token))
                .isEqualTo("admin@example.com");
    }

    @Test
    void shouldValidateTokenForMatchingEmail() {
        User user = new User(
                "Test Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(
                token,
                "approver@example.com"
        )).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentEmail() {
        User user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(
                token,
                "another@example.com"
        )).isFalse();
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        User user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        String token = jwtService.generateToken(user);

        byte[] differentKey = new byte[32];
        differentKey[0] = 1;

        JwtService anotherJwtService = new JwtService(
                Base64.getEncoder().encodeToString(differentKey),
                86400000
        );

        assertThatThrownBy(() -> anotherJwtService.extractEmail(token))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService expiredJwtService = new JwtService(
                SECRET,
                -1000
        );

        User user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        String token = expiredJwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(RuntimeException.class);
    }
}

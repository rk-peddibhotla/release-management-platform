
package com.ravikanth.releasemanagement.auth;

import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );
    }

    @Test
    void shouldLoginWithValidCredentials() {

        LoginRequest request = new LoginRequest();
        request.setEmail("engineer@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenReturn(authentication);

        when(authentication.getName())
                .thenReturn("engineer@example.com");

        when(userRepository.findByEmail("engineer@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getName()).isEqualTo("Test Engineer");
        assertThat(response.getEmail())
                .isEqualTo("engineer@example.com");
        assertThat(response.getRole()).isEqualTo("ENGINEER");

        verify(authenticationManager).authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));
        verify(jwtService).generateToken(user);
    }

    @Test
    void shouldNormalizeEmailBeforeAuthentication() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  ENGINEER@EXAMPLE.COM  ");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenReturn(authentication);

        when(authentication.getName())
                .thenReturn("engineer@example.com");

        when(userRepository.findByEmail("engineer@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getEmail())
                .isEqualTo("engineer@example.com");
    }

    @Test
    void shouldRejectInvalidCredentials() {

        LoginRequest request = new LoginRequest();
        request.setEmail("engineer@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid email or password."
                )
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> authService.login(request)
        ).isInstanceOf(
                org.springframework.security.authentication.BadCredentialsException.class
        );

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserCannotBeFound() {

        LoginRequest request = new LoginRequest();
        request.setEmail("engineer@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenReturn(authentication);

        when(authentication.getName())
                .thenReturn("engineer@example.com");

        when(userRepository.findByEmail("engineer@example.com"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> authService.login(request)
        ).isInstanceOf(IllegalStateException.class);

        verify(jwtService, never()).generateToken(any());
    }
}

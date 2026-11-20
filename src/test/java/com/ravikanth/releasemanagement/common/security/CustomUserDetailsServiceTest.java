
package com.ravikanth.releasemanagement.common.security;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByEmail() {
        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                userDetailsService.loadUserByUsername("test@example.com");

        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ENGINEER");
        assertThat(result.isEnabled()).isTrue();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void shouldNormalizeEmailBeforeLoadingUser() {
        User user = new User(
                "Test Admin",
                "admin@example.com",
                "hashed-password",
                UserRole.ADMIN
        );

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                userDetailsService.loadUserByUsername(" ADMIN@Example.com ");

        assertThat(result.getUsername()).isEqualTo("admin@example.com");

        verify(userRepository).findByEmail("admin@example.com");
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("unknown@example.com")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid email or password.");

        verify(userRepository).findByEmail("unknown@example.com");
    }

    @Test
    void shouldDisableInactiveUser() {
        User user = new User(
                "Inactive Engineer",
                "inactive@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        user.deactivate();

        when(userRepository.findByEmail("inactive@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                userDetailsService.loadUserByUsername("inactive@example.com");

        assertThat(result.isEnabled()).isFalse();

        verify(userRepository).findByEmail("inactive@example.com");
    }

    @Test
    void shouldLoadApproverRoleCorrectly() {
        User user = new User(
                "Test Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        when(userRepository.findByEmail("approver@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                userDetailsService.loadUserByUsername("approver@example.com");

        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_APPROVER");
    }
}

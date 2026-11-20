
package com.ravikanth.releasemanagement.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("raw-password"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser(
                "  Test Engineer  ",
                " TEST@Example.com ",
                "raw-password",
                UserRole.ENGINEER
        );

        assertThat(result.getName()).isEqualTo("Test Engineer");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("raw-password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                "Test Engineer",
                "TEST@example.com",
                "raw-password",
                UserRole.ENGINEER
        ))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> userService.createUser(
                " ",
                "test@example.com",
                "password",
                UserRole.ENGINEER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be blank.");
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThatThrownBy(() -> userService.createUser(
                "Test Engineer",
                " ",
                "password",
                UserRole.ENGINEER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be blank.");
    }

    @Test
    void shouldRejectBlankPassword() {
        assertThatThrownBy(() -> userService.createUser(
                "Test Engineer",
                "test@example.com",
                " ",
                UserRole.ENGINEER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password cannot be blank.");
    }

    @Test
    void shouldRejectNullRole() {
        assertThatThrownBy(() -> userService.createUser(
                "Test Engineer",
                "test@example.com",
                "password",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User role cannot be null.");
    }

    @Test
    void shouldReturnUserWhenIdExists() {
        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserIdDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with ID 99 was not found.");
    }

    @Test
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must be positive.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldRejectZeroUserId() {
        assertThatThrownBy(() -> userService.getUserById(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must be positive.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldRejectNegativeUserId() {
        assertThatThrownBy(() -> userService.getUserById(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must be positive.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldReturnUserWhenEmailExists() {
        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        User result = userService.getUserByEmail(" TEST@Example.com ");

        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void shouldThrowExceptionWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(
                "unknown@example.com"
        ))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with email 'unknown@example.com' was not found.");
    }

    @Test
    void shouldReturnPaginatedUsers() {
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        Page<User> userPage = new PageImpl<>(
                List.of(user),
                pageable,
                1
        );

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<User> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail())
                .isEqualTo("test@example.com");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(userRepository).findAll(pageable);
    }

    @Test
    void shouldDeactivateUser() {
        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.deactivateUser(1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldActivateUser() {
        User user = new User(
                "Test Engineer",
                "test@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        user.deactivate();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.activateUser(1L);

        assertThat(result.isActive()).isTrue();
    }
}

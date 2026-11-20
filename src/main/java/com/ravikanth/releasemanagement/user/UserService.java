
package com.ravikanth.releasemanagement.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(
            String name,
            String email,
            String rawPassword,
            UserRole role
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank.");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }

        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }

        String normalizedName = name.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                normalizedName,
                normalizedEmail,
                passwordHash,
                role
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        validateUserId(id);

        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User deactivateUser(Long id) {
        validateUserId(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.deactivate();

        return user;
    }

    @Transactional
    public User activateUser(Long id) {
        validateUserId(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.activate();

        return user;
    }

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
    }
}

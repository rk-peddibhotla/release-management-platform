package com.ravikanth.releasemanagement.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User(
                "Test Engineer",
                "test.engineer@example.com",
                "test-password-hash",
                UserRole.ENGINEER
        );

        userRepository.save(user);

        var savedUser = userRepository.findByEmail(
                "test.engineer@example.com"
        );

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("Test Engineer");
        assertThat(savedUser.get().getRole()).isEqualTo(UserRole.ENGINEER);
    }

    @Test
    void shouldCheckWhetherEmailExists() {
        User user = new User(
                "Test Admin",
                "test.admin@example.com",
                "test-password-hash",
                UserRole.ADMIN
        );

        userRepository.save(user);

        assertThat(userRepository.existsByEmail(
                "test.admin@example.com"
        )).isTrue();

        assertThat(userRepository.existsByEmail(
                "unknown@example.com"
        )).isFalse();
    }
}

package com.ravikanth.releasemanagement.user;

import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.user.dto.UserResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class
)
@Import(UserControllerTest.TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // GET /api/users/{id}
    // -------------------------------------------------------------------------

    @Test
    void adminGetsUserById() throws Exception {
        User user = new User(
                "John Doe",
                "john@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void engineerCannotGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/1")
                        .with(user("engineer@example.com").roles("ENGINEER")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /api/users
    // -------------------------------------------------------------------------

    @Test
    void adminListsUsers() throws Exception {
        User user = new User(
                "John Doe",
                "john@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        Page<User> page = new PageImpl<>(
                List.of(user),
                PageRequest.of(0, 20),
                1
        );

        when(userService.getAllUsers(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/users")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"))
                .andExpect(jsonPath("$.content[0].role").value("ENGINEER"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void engineerCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("engineer@example.com").roles("ENGINEER")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /api/users
    // -------------------------------------------------------------------------

    @Test
    void adminCreatesUser() throws Exception {
        User createdUser = new User(
                "New User",
                "newuser@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        when(userService.createUser(
                eq("New User"),
                eq("newuser@example.com"),
                eq("Password123"),
                eq(UserRole.ENGINEER)
        )).thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "New User",
                                    "email": "newuser@example.com",
                                    "password": "Password123",
                                    "role": "ENGINEER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void adminCannotCreateUserWithInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "email": "not-an-email",
                                    "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/{id}/activate
    // -------------------------------------------------------------------------

    @Test
    void adminActivatesUser() throws Exception {
        User user = new User(
                "User",
                "user@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        user.activate();

        when(userService.activateUser(10L)).thenReturn(user);

        mockMvc.perform(patch("/api/users/10/activate")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/{id}/deactivate
    // -------------------------------------------------------------------------

    @Test
    void adminDeactivatesUser() throws Exception {
        User user = new User(
                "User",
                "user@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        user.deactivate();

        when(userService.deactivateUser(10L)).thenReturn(user);

        mockMvc.perform(patch("/api/users/10/deactivate")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.active").value(false));
    }

    // -------------------------------------------------------------------------
    // Test security configuration
    // -------------------------------------------------------------------------

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(
                HttpSecurity http
        ) throws Exception {

            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth ->
                            auth.anyRequest().authenticated()
                    );

            return http.build();
        }
    }
}
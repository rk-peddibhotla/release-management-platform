
package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentType;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;
import com.ravikanth.releasemanagement.user.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReleaseController.class)
@Import(ReleaseControllerTest.TestSecurityConfig.class)
class ReleaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReleaseService releaseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private User authenticatedEngineer() {
        return new User(
                "John Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );
    }

    private Environment sampleEnvironment() {
        return new Environment(
                new Application(
                        "Payment Service",
                        "Handles payment processing."
                ),
                "Production",
                EnvironmentType.PRODUCTION
        );
    }

    private Release sampleRelease() {
        return new Release(
                sampleEnvironment(),
                "1.0.0",
                "Initial release",
                OffsetDateTime.parse("2026-12-01T10:00:00Z"),
                authenticatedEngineer()
        );
    }

    // GET /api/releases/{id}

    @Test
    void adminGetsReleaseById() throws Exception {
        when(releaseService.getReleaseById(1L))
                .thenReturn(sampleRelease());

        mockMvc.perform(
                        get("/api/releases/1")
                                .with(user("admin@example.com").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.description")
                        .value("Initial release"));
    }

    // GET /api/environments/{environmentId}/releases

    @Test
    void engineerListsReleasesByEnvironment() throws Exception {
        Page<Release> page = new PageImpl<>(
                List.of(sampleRelease()),
                PageRequest.of(0, 20),
                1
        );

        when(releaseService.getReleasesByEnvironment(
                eq(1L),
                any()
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/environments/1/releases")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].version")
                        .value("1.0.0"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void approverCannotListReleases() throws Exception {
        mockMvc.perform(
                        get("/api/environments/1/releases")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                )
                .andExpect(status().isForbidden());
    }

    // POST /api/environments/{environmentId}/releases

    @Test
    void engineerCreatesRelease() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(releaseService.createRelease(
                eq(1L),
                eq("1.0.0"),
                eq("Initial release"),
                any(),
                any()
        )).thenReturn(sampleRelease());

        mockMvc.perform(
                        post("/api/environments/1/releases")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "version": "1.0.0",
                                            "description": "Initial release",
                                            "scheduledAt": "2026-12-01T10:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(releaseService).createRelease(
                eq(1L),
                eq("1.0.0"),
                eq("Initial release"),
                any(),
                any()
        );
    }

    @Test
    void adminCannotCreateReleaseWithBlankVersion()
            throws Exception {

        mockMvc.perform(
                        post("/api/environments/1/releases")
                                .with(user("admin@example.com").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "version": "",
                                            "description": "Initial release"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // PUT /api/releases/{id}

    @Test
    void engineerUpdatesRelease() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(releaseService.updateRelease(
                eq(1L),
                eq("1.1.0"),
                eq("Updated release"),
                any(),
                any()
        )).thenReturn(new Release(
                sampleEnvironment(),
                "1.1.0",
                "Updated release",
                OffsetDateTime.parse("2026-12-02T10:00:00Z"),
                authenticatedEngineer()
        ));

        mockMvc.perform(
                        put("/api/releases/1")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "version": "1.1.0",
                                            "description": "Updated release",
                                            "scheduledAt": "2026-12-02T10:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.1.0"))
                .andExpect(jsonPath("$.description")
                        .value("Updated release"));
    }

    // PATCH /api/releases/{id}/cancel

    @Test
    void adminCancelsRelease() throws Exception {
        Release release = sampleRelease();
        release.cancel();

        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(new User(
                        "Admin",
                        "admin@example.com",
                        "hashed-password",
                        UserRole.ADMIN
                ));

        when(releaseService.cancelRelease(eq(1L), any()))
                .thenReturn(release);

        mockMvc.perform(
                        patch("/api/releases/1/cancel")
                                .with(user("admin@example.com").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // POST /api/releases/{releaseId}/change-requests/{changeRequestId}

    @Test
    void engineerAddsChangeRequestToRelease() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(releaseService.addChangeRequest(
                eq(1L),
                eq(10L),
                any()
        )).thenReturn(sampleRelease());

        mockMvc.perform(
                        post("/api/releases/1/change-requests/10")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));

        verify(releaseService).addChangeRequest(
                eq(1L),
                eq(10L),
                any()
        );
    }

    // DELETE /api/releases/{releaseId}/change-requests/{changeRequestId}

    @Test
    void engineerRemovesChangeRequestFromRelease() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(releaseService.removeChangeRequest(
                eq(1L),
                eq(10L),
                any()
        )).thenReturn(sampleRelease());

        mockMvc.perform(
                        delete("/api/releases/1/change-requests/10")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    // PATCH /api/releases/{id}/start

    @Test
    void engineerStartsRelease() throws Exception {
        Release release = sampleRelease();
        release.start();

        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(releaseService.startDeployment(eq(1L), any()))
                .thenReturn(release);

        mockMvc.perform(
                        patch("/api/releases/1/start")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // Test security configuration

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

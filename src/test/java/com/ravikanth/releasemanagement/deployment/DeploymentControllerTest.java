
package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeploymentController.class)
@Import(DeploymentControllerTest.TestSecurityConfig.class)
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeploymentService deploymentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private User authenticatedEngineer() {
        User engineer = mock(User.class);

        when(engineer.getId()).thenReturn(2L);

        return engineer;
    }

    private Deployment sampleDeployment(
            DeploymentStatus status,
            String notes
    ) {
        Deployment deployment = mock(Deployment.class);
        Release release = mock(Release.class);
        User engineer = authenticatedEngineer();

        when(deployment.getId()).thenReturn(10L);
        when(deployment.getRelease()).thenReturn(release);
        when(release.getId()).thenReturn(1L);
        when(deployment.getAttemptNumber()).thenReturn(1);
        when(deployment.getStatus()).thenReturn(status);
        when(deployment.getDeployedBy()).thenReturn(engineer);
        when(deployment.getStartedAt()).thenReturn(
                OffsetDateTime.parse("2026-11-19T10:00:00Z")
        );
        when(deployment.getCompletedAt()).thenReturn(
                OffsetDateTime.parse("2026-11-19T10:30:00Z")
        );
        when(deployment.getNotes()).thenReturn(notes);

        return deployment;
    }

    // POST /api/releases/{releaseId}/deployments

    @Test
    void engineerRecordsSuccessfulDeployment() throws Exception {
        User engineer = authenticatedEngineer();

        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(engineer);

        Deployment deployment = sampleDeployment(
                DeploymentStatus.SUCCESS,
                "Deployment test"
        );

        when(deploymentService.recordDeployment(
                eq(1L),
                eq(2L),
                eq(DeploymentStatus.SUCCESS),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                eq("Deployment test")
        )).thenReturn(deployment);

        mockMvc.perform(
                        post("/api/releases/1/deployments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "status": "SUCCESS",
                                            "startedAt": "2026-11-19T10:00:00Z",
                                            "completedAt": "2026-11-19T10:30:00Z",
                                            "notes": "Deployment test"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.releaseId").value(1))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.deployedById").value(2))
                .andExpect(jsonPath("$.notes").value("Deployment test"));

        verify(deploymentService).recordDeployment(
                eq(1L),
                eq(2L),
                eq(DeploymentStatus.SUCCESS),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                eq("Deployment test")
        );
    }

    @Test
    void engineerRecordsFailedDeployment() throws Exception {
        User engineer = authenticatedEngineer();

        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(engineer);

        Deployment deployment = sampleDeployment(
                DeploymentStatus.FAILED,
                "Deployment failed"
        );

        when(deploymentService.recordDeployment(
                eq(1L),
                eq(2L),
                eq(DeploymentStatus.FAILED),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                eq("Deployment failed")
        )).thenReturn(deployment);

        mockMvc.perform(
                        post("/api/releases/1/deployments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "status": "FAILED",
                                            "startedAt": "2026-11-19T10:00:00Z",
                                            "completedAt": "2026-11-19T10:30:00Z",
                                            "notes": "Deployment failed"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.deployedById").value(2))
                .andExpect(jsonPath("$.notes").value("Deployment failed"));

        verify(deploymentService).recordDeployment(
                eq(1L),
                eq(2L),
                eq(DeploymentStatus.FAILED),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                eq("Deployment failed")
        );
    }

    @Test
    void engineerCannotRecordDeploymentWithMissingStatus()
            throws Exception {

        mockMvc.perform(
                        post("/api/releases/1/deployments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "startedAt": "2026-11-19T10:00:00Z",
                                            "completedAt": "2026-11-19T10:30:00Z",
                                            "notes": "Deployment test"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    // GET /api/deployments/{id}

    @Test
    void adminGetsDeploymentById() throws Exception {

        Deployment deployment = sampleDeployment(
                DeploymentStatus.SUCCESS,
                "Deployment test"
        );

        when(deploymentService.getDeploymentById(10L))
                .thenReturn(deployment);

        mockMvc.perform(
                        get("/api/deployments/10")
                                .with(user("admin@example.com")
                                        .roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.releaseId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.deployedById").value(2))
                .andExpect(jsonPath("$.notes").value("Deployment test"));
    }

    // GET /api/releases/{releaseId}/deployments

    @Test
    void engineerGetsDeploymentHistory() throws Exception {
        List<Deployment> deployments = List.of(
                sampleDeployment(
                        DeploymentStatus.FAILED,
                        "Deployment failed"
                ),
                sampleDeployment(
                        DeploymentStatus.SUCCESS,
                        "Deployment succeeded"
                )
        );

        when(deploymentService.getDeploymentsByReleaseId(1L))
                .thenReturn(deployments);

        mockMvc.perform(
                        get("/api/releases/1/deployments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].notes").value("Deployment failed"))
                .andExpect(jsonPath("$[1].notes").value("Deployment succeeded"));
    }

    // Authorization

    @Test
    void approverCannotAccessDeploymentEndpoints() throws Exception {
        mockMvc.perform(
                        get("/api/deployments/10")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                )
                .andExpect(status().isForbidden());
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

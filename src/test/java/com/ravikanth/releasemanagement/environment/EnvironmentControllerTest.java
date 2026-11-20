
package com.ravikanth.releasemanagement.environment;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.environment.dto.EnvironmentResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EnvironmentController.class)
@Import(EnvironmentControllerTest.TestSecurityConfig.class)
class EnvironmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvironmentService environmentService;

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
        Application application = new Application(
                "Payment Service",
                "Handles payment processing."
        );

        return new Environment(
                application,
                "Production",
                EnvironmentType.PRODUCTION
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/environments/{id}
    // -------------------------------------------------------------------------

    @Test
    void adminGetsEnvironmentById() throws Exception {
        when(environmentService.getEnvironmentById(1L))
                .thenReturn(sampleEnvironment());

        mockMvc.perform(get("/api/environments/1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.environmentType").value("PRODUCTION"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.applicationName")
                        .value("Payment Service"));
    }

    @Test
    void engineerGetsEnvironmentById() throws Exception {
        when(environmentService.getEnvironmentById(1L))
                .thenReturn(sampleEnvironment());

        mockMvc.perform(get("/api/environments/1")
                        .with(user("engineer@example.com").roles("ENGINEER")))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /api/applications/{applicationId}/environments
    // -------------------------------------------------------------------------

    @Test
    void engineerListsEnvironmentsByApplication() throws Exception {
        Page<Environment> page = new PageImpl<>(
                List.of(sampleEnvironment()),
                PageRequest.of(0, 20),
                1
        );

        when(environmentService.getEnvironmentsByApplication(
                eq(1L),
                any()
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/applications/1/environments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Production"))
                .andExpect(jsonPath("$.content[0].environmentType")
                        .value("PRODUCTION"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void approverCannotListEnvironments() throws Exception {
        mockMvc.perform(
                        get("/api/applications/1/environments")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                )
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /api/applications/{applicationId}/environments
    // -------------------------------------------------------------------------

    @Test
    void engineerCreatesEnvironment() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(environmentService.createEnvironment(
                eq(1L),
                eq("Production"),
                eq(EnvironmentType.PRODUCTION),
                any()
        )).thenReturn(sampleEnvironment());

        mockMvc.perform(
                        post("/api/applications/1/environments")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Production",
                                            "environmentType": "PRODUCTION"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.environmentType")
                        .value("PRODUCTION"));

        verify(environmentService).createEnvironment(
                eq(1L),
                eq("Production"),
                eq(EnvironmentType.PRODUCTION),
                any()
        );
    }

    @Test
    void adminCannotCreateEnvironmentWithInvalidRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/applications/1/environments")
                                .with(user("admin@example.com").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "",
                                            "environmentType": "PRODUCTION"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void adminCannotCreateEnvironmentWithoutEnvironmentType()
            throws Exception {

        mockMvc.perform(
                        post("/api/applications/1/environments")
                                .with(user("admin@example.com").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Production"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/environments/{id}
    // -------------------------------------------------------------------------

    @Test
    void engineerUpdatesEnvironment() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(environmentService.updateEnvironment(
                eq(1L),
                eq("Staging"),
                eq(EnvironmentType.STAGING),
                any()
        )).thenReturn(new Environment(
                new Application("Payment Service", "Handles payments."),
                "Staging",
                EnvironmentType.STAGING
        ));

        mockMvc.perform(
                        put("/api/environments/1")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Staging",
                                            "environmentType": "STAGING"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Staging"))
                .andExpect(jsonPath("$.environmentType").value("STAGING"));
    }

    @Test
    void adminCannotUpdateEnvironmentWithInvalidRequest()
            throws Exception {

        mockMvc.perform(
                        put("/api/environments/1")
                                .with(user("admin@example.com").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "",
                                            "environmentType": "STAGING"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/environments/{id}/activate
    // -------------------------------------------------------------------------

    @Test
    void adminActivatesEnvironment() throws Exception {
        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(new User(
                        "Admin",
                        "admin@example.com",
                        "hashed-password",
                        UserRole.ADMIN
                ));

        when(environmentService.activateEnvironment(eq(1L), any()))
                .thenReturn(sampleEnvironment());

        mockMvc.perform(
                        patch("/api/environments/1/activate")
                                .with(user("admin@example.com").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/environments/{id}/deactivate
    // -------------------------------------------------------------------------

    @Test
    void adminDeactivatesEnvironment() throws Exception {
        Environment environment = sampleEnvironment();
        environment.deactivate();

        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(new User(
                        "Admin",
                        "admin@example.com",
                        "hashed-password",
                        UserRole.ADMIN
                ));

        when(environmentService.deactivateEnvironment(eq(1L), any()))
                .thenReturn(environment);

        mockMvc.perform(
                        patch("/api/environments/1/deactivate")
                                .with(user("admin@example.com").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Production"))
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

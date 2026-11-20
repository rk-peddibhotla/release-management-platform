
package com.ravikanth.releasemanagement.application;

import com.ravikanth.releasemanagement.common.security.JwtService;
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

@WebMvcTest(controllers = ApplicationController.class)
@Import(ApplicationControllerTest.TestSecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

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

    private Application sampleApplication() {
        return new Application(
                "Payment Service",
                "Handles payment processing."
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/applications/{id}
    // -------------------------------------------------------------------------

    @Test
    void adminGetsApplicationById() throws Exception {
        when(applicationService.getApplicationById(1L))
                .thenReturn(sampleApplication());

        mockMvc.perform(get("/api/applications/1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payment Service"))
                .andExpect(jsonPath("$.description")
                        .value("Handles payment processing."))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void engineerGetsApplicationById() throws Exception {
        when(applicationService.getApplicationById(1L))
                .thenReturn(sampleApplication());

        mockMvc.perform(get("/api/applications/1")
                        .with(user("engineer@example.com").roles("ENGINEER")))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /api/applications
    // -------------------------------------------------------------------------

    @Test
    void engineerListsApplications() throws Exception {
        Page<Application> page = new PageImpl<>(
                List.of(sampleApplication()),
                PageRequest.of(0, 20),
                1
        );

        when(applicationService.getAllApplications(any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/applications")
                        .with(user("engineer@example.com").roles("ENGINEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Payment Service"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void approverCannotListApplications() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .with(user("approver@example.com").roles("APPROVER")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /api/applications/by-name
    // -------------------------------------------------------------------------

    @Test
    void adminGetsApplicationByName() throws Exception {
        when(applicationService.getApplicationByName("Payment Service"))
                .thenReturn(sampleApplication());

        mockMvc.perform(get("/api/applications/by-name")
                        .param("name", "Payment Service")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payment Service"));
    }

    // -------------------------------------------------------------------------
    // POST /api/applications
    // -------------------------------------------------------------------------

    @Test
    void engineerCreatesApplication() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(applicationService.createApplication(
                eq("Payment Service"),
                eq("Handles payment processing."),
                any()
        )).thenReturn(sampleApplication());

        mockMvc.perform(post("/api/applications")
                        .with(user("engineer@example.com").roles("ENGINEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Payment Service",
                                    "description": "Handles payment processing."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Payment Service"))
                .andExpect(jsonPath("$.description")
                        .value("Handles payment processing."));

        verify(applicationService).createApplication(
                eq("Payment Service"),
                eq("Handles payment processing."),
                any()
        );
    }

    @Test
    void adminCannotCreateApplicationWithInvalidRequest()
            throws Exception {

        mockMvc.perform(post("/api/applications")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "description": "Invalid application"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/applications/{id}
    // -------------------------------------------------------------------------

    @Test
    void engineerUpdatesApplication() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(applicationService.updateApplication(
                eq(1L),
                eq("Updated Payment Service"),
                eq("Updated description."),
                any()
        )).thenReturn(new Application(
                "Updated Payment Service",
                "Updated description."
        ));

        mockMvc.perform(put("/api/applications/1")
                        .with(user("engineer@example.com").roles("ENGINEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Updated Payment Service",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Payment Service"));
    }

    @Test
    void adminCannotUpdateApplicationWithInvalidRequest()
            throws Exception {

        mockMvc.perform(put("/api/applications/1")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/applications/{id}/activate
    // -------------------------------------------------------------------------

    @Test
    void adminActivatesApplication() throws Exception {
        Application application = sampleApplication();

        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(new User(
                        "Admin",
                        "admin@example.com",
                        "hashed-password",
                        UserRole.ADMIN
                ));

        when(applicationService.activateApplication(eq(1L), any()))
                .thenReturn(application);

        mockMvc.perform(patch("/api/applications/1/activate")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payment Service"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/applications/{id}/deactivate
    // -------------------------------------------------------------------------

    @Test
    void adminDeactivatesApplication() throws Exception {
        Application application = sampleApplication();
        application.deactivate();

        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(new User(
                        "Admin",
                        "admin@example.com",
                        "hashed-password",
                        UserRole.ADMIN
                ));

        when(applicationService.deactivateApplication(eq(1L), any()))
                .thenReturn(application);

        mockMvc.perform(patch("/api/applications/1/deactivate")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payment Service"))
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

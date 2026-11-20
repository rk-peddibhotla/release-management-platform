
package com.ravikanth.releasemanagement.changerequest;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.changerequest.dto.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChangeRequestController.class)
@Import(ChangeRequestControllerTest.TestSecurityConfig.class)
class ChangeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChangeRequestService changeRequestService;

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

    private ChangeRequest sampleChangeRequest() {
        return new ChangeRequest(
                sampleEnvironment(),
                "Update payment timeout",
                "Increase payment timeout configuration.",
                ChangeType.CONFIGURATION,
                authenticatedEngineer(),
                OffsetDateTime.parse("2026-12-01T10:00:00Z")
        );
    }

    // GET /api/change-requests/{id}

    @Test
    void adminGetsChangeRequestById() throws Exception {
        when(changeRequestService.getChangeRequestById(1L))
                .thenReturn(sampleChangeRequest());

        mockMvc.perform(
                        get("/api/change-requests/1")
                                .with(user("admin@example.com")
                                        .roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Update payment timeout"))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.changeType")
                        .value("CONFIGURATION"));
    }

    // GET /api/environments/{environmentId}/change-requests

    @Test
    void engineerListsChangeRequestsByEnvironment()
            throws Exception {

        Page<ChangeRequest> page = new PageImpl<>(
                List.of(sampleChangeRequest()),
                PageRequest.of(0, 20),
                1
        );

        when(changeRequestService.getChangeRequestsByEnvironment(
                eq(1L),
                any()
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/environments/1/change-requests")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title")
                        .value("Update payment timeout"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }

    @Test
    void approverCannotListChangeRequestsByEnvironment()
            throws Exception {

        mockMvc.perform(
                        get("/api/environments/1/change-requests")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                )
                .andExpect(status().isForbidden());
    }

    // GET /api/users/{userId}/change-requests

    @Test
    void engineerListsChangeRequestsByRequester()
            throws Exception {

        Page<ChangeRequest> page = new PageImpl<>(
                List.of(sampleChangeRequest()),
                PageRequest.of(0, 20),
                1
        );

        when(changeRequestService.getChangeRequestsByRequester(
                eq(1L),
                any()
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/users/1/change-requests")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title")
                        .value("Update payment timeout"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }

    // POST /api/environments/{environmentId}/change-requests

    @Test
    void engineerCreatesChangeRequest() throws Exception {
        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(changeRequestService.createChangeRequest(
                eq(1L),
                eq("Update payment timeout"),
                eq("Increase payment timeout configuration."),
                eq(ChangeType.CONFIGURATION),
                any(),
                any()
        )).thenReturn(sampleChangeRequest());

        mockMvc.perform(
                        post("/api/environments/1/change-requests")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Update payment timeout",
                                            "description": "Increase payment timeout configuration.",
                                            "changeType": "CONFIGURATION",
                                            "plannedAt": "2026-12-01T10:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("Update payment timeout"))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"));

        verify(changeRequestService).createChangeRequest(
                eq(1L),
                eq("Update payment timeout"),
                eq("Increase payment timeout configuration."),
                eq(ChangeType.CONFIGURATION),
                any(),
                any()
        );
    }

    @Test
    void adminCannotCreateChangeRequestWithBlankTitle()
            throws Exception {

        mockMvc.perform(
                        post("/api/environments/1/change-requests")
                                .with(user("admin@example.com")
                                        .roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "description": "Test request",
                                            "changeType": "CONFIGURATION"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors")
                        .exists());
    }

    // PUT /api/change-requests/{id}

    @Test
    void engineerUpdatesChangeRequest() throws Exception {
        when(changeRequestService.updateChangeRequest(
                eq(1L),
                eq("Updated payment timeout"),
                eq("Updated configuration."),
                eq(ChangeType.CONFIGURATION),
                any()
        )).thenReturn(new ChangeRequest(
                sampleEnvironment(),
                "Updated payment timeout",
                "Updated configuration.",
                ChangeType.CONFIGURATION,
                authenticatedEngineer(),
                OffsetDateTime.parse("2026-12-02T10:00:00Z")
        ));

        mockMvc.perform(
                        put("/api/change-requests/1")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Updated payment timeout",
                                            "description": "Updated configuration.",
                                            "changeType": "CONFIGURATION",
                                            "plannedAt": "2026-12-02T10:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Updated payment timeout"))
                .andExpect(jsonPath("$.description")
                        .value("Updated configuration."));
    }

    // PATCH /api/change-requests/{id}/submit

    @Test
    void engineerSubmitsChangeRequest() throws Exception {
        ChangeRequest changeRequest = sampleChangeRequest();
        changeRequest.submit();

        when(userService.getUserByEmail("engineer@example.com"))
                .thenReturn(authenticatedEngineer());

        when(changeRequestService.submitChangeRequest(
                eq(1L),
                any()
        )).thenReturn(changeRequest);

        mockMvc.perform(
                        patch("/api/change-requests/1/submit")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("SUBMITTED"));

        verify(changeRequestService).submitChangeRequest(
                eq(1L),
                any()
        );
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

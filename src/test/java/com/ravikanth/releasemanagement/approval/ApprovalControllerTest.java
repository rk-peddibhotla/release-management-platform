package com.ravikanth.releasemanagement.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravikanth.releasemanagement.approval.dto.ApprovalDecisionRequest;
import com.ravikanth.releasemanagement.approval.dto.ApprovalResponse;
import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApprovalController.class)
@Import(ApprovalControllerTest.TestSecurityConfig.class)
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApprovalService approvalService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void approverCanApproveChangeRequest() throws Exception {
        User approver = new User(
                "Jane Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        Approval approval = sampleApproval();

        when(userService.getUserByEmail("approver@example.com"))
                .thenReturn(approver);

        when(approvalService.approveChangeRequest(
                eq(1L),
                eq(approver.getId()),
                eq("Approved for release.")
        )).thenReturn(approval);

        ApprovalDecisionRequest request =
                new ApprovalDecisionRequest("Approved for release.");

        mockMvc.perform(
                        post("/api/change-requests/1/approve")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.comments")
                        .value("Approved for release."));

        verify(approvalService).approveChangeRequest(
                1L,
                approver.getId(),
                "Approved for release."
        );
    }

    @Test
    void approverCanRejectChangeRequest() throws Exception {
        User approver = new User(
                "Jane Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        Approval approval = sampleApproval();

        when(userService.getUserByEmail("approver@example.com"))
                .thenReturn(approver);

        when(approvalService.rejectChangeRequest(
                eq(1L),
                eq(approver.getId()),
                eq("Needs more testing.")
        )).thenReturn(approval);

        ApprovalDecisionRequest request =
                new ApprovalDecisionRequest("Needs more testing.");

        mockMvc.perform(
                        post("/api/change-requests/1/reject")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(approvalService).rejectChangeRequest(
                1L,
                approver.getId(),
                "Needs more testing."
        );
    }

    @Test
    void adminCanApproveChangeRequest() throws Exception {
        User admin = new User(
                "Admin User",
                "admin@example.com",
                "hashed-password",
                UserRole.ADMIN
        );

        Approval approval = sampleApproval();

        when(userService.getUserByEmail("admin@example.com"))
                .thenReturn(admin);

        when(approvalService.approveChangeRequest(
                eq(1L),
                eq(admin.getId()),
                eq("Approved by admin.")
        )).thenReturn(approval);

        ApprovalDecisionRequest request =
                new ApprovalDecisionRequest("Approved by admin.");

        mockMvc.perform(
                        post("/api/change-requests/1/approve")
                                .with(user("admin@example.com")
                                        .roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }


    @Test
    void commentsAreOptional() throws Exception {
        User approver = new User(
                "Jane Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        Approval approval = sampleApproval();

        when(userService.getUserByEmail("approver@example.com"))
                .thenReturn(approver);

        when(approvalService.approveChangeRequest(
                eq(1L),
                eq(approver.getId()),
                eq(null)
        )).thenReturn(approval);

        mockMvc.perform(
                        post("/api/change-requests/1/approve")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        verify(approvalService).approveChangeRequest(
                1L,
                approver.getId(),
                null
        );
    }

    private Approval sampleApproval() {
        Environment environment = new Environment(
                null,
                "Production",
                com.ravikanth.releasemanagement.environment.EnvironmentType.PRODUCTION
        );

        User requester = new User(
                "John Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        User approver = new User(
                "Jane Approver",
                "approver@example.com",
                "hashed-password",
                UserRole.APPROVER
        );

        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Deploy payment fix",
                "Fix payment processing issue",
                com.ravikanth.releasemanagement.changerequest.ChangeType.BUG_FIX,
                requester,
                OffsetDateTime.now()
        );

        changeRequest.submit();
        changeRequest.approve();

        return new Approval(
                changeRequest,
                approver,
                ApprovalDecision.APPROVED,
                "Approved for release."
        );
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        SecurityFilterChain testSecurityFilterChain(
                HttpSecurity http
        ) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }
}



package com.ravikanth.releasemanagement.common.audit;

import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;




import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@Import(AuditLogControllerTest.TestSecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    

    @Test
    void userCanViewEntityAuditHistory() throws Exception {
        AuditLog auditLog = sampleAuditLog();

        when(auditLogService.getAuditHistory(
                eq("RELEASE"),
                eq(2L),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(auditLog),
                        PageRequest.of(0, 20),
                        1
                )
        );

        mockMvc.perform(
                        get("/api/audit-logs/entity/RELEASE/2")
                                .with(user("engineer@example.com")
                                        .roles("ENGINEER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userName")
                        .value("Test Engineer"))
                .andExpect(jsonPath("$.content[0].entityType")
                        .value("RELEASE"))
                .andExpect(jsonPath("$.content[0].entityId").value(2))
                .andExpect(jsonPath("$.content[0].action")
                        .value("RELEASE_CREATED"))
                .andExpect(jsonPath("$.content[0].details")
                        .value("{\"message\":\"Release created\"}"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService).getAuditHistory(
                eq("RELEASE"),
                eq(2L),
                any(Pageable.class)
        );
    }

    @Test
    void approverCanViewUserAuditHistory() throws Exception {
        AuditLog auditLog = sampleAuditLog();

        when(auditLogService.getAuditHistoryByUser(
                eq(2L),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(auditLog),
                        PageRequest.of(0, 20),
                        1
                )
        );

        mockMvc.perform(
                        get("/api/audit-logs/user/2")
                                .with(user("approver@example.com")
                                        .roles("APPROVER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userName")
                        .value("Test Engineer"))
                .andExpect(jsonPath("$.content[0].entityType")
                        .value("RELEASE"))
                .andExpect(jsonPath("$.content[0].entityId").value(2))
                .andExpect(jsonPath("$.content[0].action")
                        .value("RELEASE_CREATED"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService).getAuditHistoryByUser(
                eq(2L),
                any(Pageable.class)
        );
    }

    private AuditLog sampleAuditLog() {
        User user = new User(
                "Test Engineer",
                "engineer@example.com",
                "hashed-password",
                UserRole.ENGINEER
        );

        return new AuditLog(
            user,
            "RELEASE",
            2L,
            "RELEASE_CREATED",
            "{\"message\":\"Release created\"}" 

        );
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

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
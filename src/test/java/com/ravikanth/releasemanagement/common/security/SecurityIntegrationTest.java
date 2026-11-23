
package com.ravikanth.releasemanagement.common.security;

import com.ravikanth.releasemanagement.application.ApplicationService;
import com.ravikanth.releasemanagement.approval.ApprovalService;
import com.ravikanth.releasemanagement.auth.AuthService;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestService;
import com.ravikanth.releasemanagement.common.config.PasswordConfig;
import com.ravikanth.releasemanagement.common.config.SecurityConfig;
import com.ravikanth.releasemanagement.common.exception.GlobalExceptionHandler;
import com.ravikanth.releasemanagement.deployment.DeploymentService;
import com.ravikanth.releasemanagement.environment.EnvironmentService;
import com.ravikanth.releasemanagement.release.ReleaseService;
import com.ravikanth.releasemanagement.user.UserService;



import com.ravikanth.releasemanagement.common.audit.AuditLogService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private ReleaseService releaseService;

    @MockitoBean
    private ChangeRequestService changeRequestService;

    @MockitoBean
    private ApprovalService approvalService;

    @MockitoBean
    private DeploymentService deploymentService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void shouldReturn401WhenAuthorizationHeaderIsMissing()
            throws Exception {

        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/test/protected"));
    }

    @Test
    void shouldReturn401WhenJwtIsInvalid()
            throws Exception {

        org.mockito.Mockito.when(
                jwtService.extractEmail("invalid.token.value")
        ).thenThrow(new io.jsonwebtoken.MalformedJwtException(
                "Invalid token"
        ));

        mockMvc.perform(
                        get("/api/test/protected")
                                .header(
                                        "Authorization",
                                        "Bearer invalid.token.value"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/test/protected"));
    }
}

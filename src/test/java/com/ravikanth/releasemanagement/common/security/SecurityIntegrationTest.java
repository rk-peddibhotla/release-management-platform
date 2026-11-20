
package com.ravikanth.releasemanagement.common.security;

import com.ravikanth.releasemanagement.common.config.PasswordConfig;
import com.ravikanth.releasemanagement.common.config.SecurityConfig;
import com.ravikanth.releasemanagement.common.exception.GlobalExceptionHandler;
import com.ravikanth.releasemanagement.user.UserService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ravikanth.releasemanagement.common.security.JwtAuthenticationFilter;
import com.ravikanth.releasemanagement.common.security.JwtService;
import com.ravikanth.releasemanagement.common.security.RestAccessDeniedHandler;
import com.ravikanth.releasemanagement.common.security.RestAuthenticationEntryPoint;

import com.ravikanth.releasemanagement.application.ApplicationService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ravikanth.releasemanagement.auth.AuthService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.userdetails.UserDetailsService;

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

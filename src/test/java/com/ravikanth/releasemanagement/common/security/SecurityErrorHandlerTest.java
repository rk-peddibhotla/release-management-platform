
package com.ravikanth.releasemanagement.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class SecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestAuthenticationEntryPoint authenticationEntryPoint =
            new RestAuthenticationEntryPoint(objectMapper);

    private final RestAccessDeniedHandler accessDeniedHandler =
            new RestAccessDeniedHandler(objectMapper);

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/releases");
        return request;
    }

    @Test
    void shouldReturn401ForUnauthenticatedRequest() throws Exception {

        HttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Invalid credentials")
        );

        String responseBody = response.getContentAsString();

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(responseBody.contains("Unauthorized"));
        assertTrue(responseBody.contains("Authentication is required"));
        assertTrue(responseBody.contains("/api/releases"));
    }

    @Test
    void shouldReturn403ForAccessDeniedRequest() throws Exception {

        HttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        String responseBody = response.getContentAsString();

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(responseBody.contains("Forbidden"));
        assertTrue(responseBody.contains("You do not have permission"));
        assertTrue(responseBody.contains("/api/releases"));
    }
}

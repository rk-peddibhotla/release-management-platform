
package com.ravikanth.releasemanagement.common.exception;
import com.ravikanth.releasemanagement.application.ApplicationAlreadyExistsException;
import com.ravikanth.releasemanagement.application.ApplicationNotFoundException;
import com.ravikanth.releasemanagement.auth.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;


class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        return request;
    }

    @Test
    void shouldReturn404ForNotFoundException() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(
                        new ApplicationNotFoundException(1L),
                        createRequest()
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals(
                "Application with ID 1 was not found.",
                response.getBody().message()
        );
        assertEquals("/api/test", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void shouldReturn409ForDuplicateException() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleConflict(
                        new ApplicationAlreadyExistsException("Inventory"),
                        createRequest()
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertTrue(response.getBody().message().contains("Inventory"));
    }

    @Test
    void shouldReturn401ForAuthenticationException() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleAuthentication(
                        new BadCredentialsException("Bad credentials"),
                        createRequest()
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals(
                "Invalid email or password.",
                response.getBody().message()
        );
    }

    
    @Test
    void shouldReturn400WithValidationErrors() throws Exception {

        LoginRequest target = new LoginRequest();

        BindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");

        bindingResult.rejectValue(
                "email",
                "NotBlank",
                "Email is required."
        );

        MethodParameter methodParameter = new MethodParameter(
                getClass().getDeclaredMethod(
                        "sampleEndpoint",
                        String.class
                ),
                0
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        methodParameter,
                        bindingResult
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleValidation(exception, createRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());

        assertEquals(
                "Email is required.",
                response.getBody().validationErrors().get("email")
        );
    }

    @Test
    void shouldReturn409ForIllegalStateException() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalState(
                        new IllegalStateException("Release cannot be completed."),
                        createRequest()
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "Release cannot be completed.",
                response.getBody().message()
        );
    }

    @Test
    void shouldReturn400ForIllegalArgumentException() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException("Invalid input."),
                        createRequest()
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid input.", response.getBody().message());
    }

    @Test
    void shouldReturn500WithoutExposingInternalExceptionDetails() {

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpectedException(
                        new RuntimeException("Database connection failed."),
                        createRequest()
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "An unexpected error occurred.",
                response.getBody().message()
        );

        assertFalse(
                response.getBody().message()
                        .contains("Database connection failed.")
        );
    }

    private void sampleEndpoint(String email) {
        // Used to obtain a MethodParameter for the validation test.
    }
}

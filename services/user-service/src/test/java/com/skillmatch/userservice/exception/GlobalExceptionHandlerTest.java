package com.skillmatch.userservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for every handler in {@link GlobalExceptionHandler}, exercised without
 * a Spring context. The domain and validation exception paths are already covered indirectly
 * by {@code UserControllerTest} and {@code AdminUserControllerTest}; this class fills in the
 * handlers that are otherwise never triggered (constraint violations, the generic fallback).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_returns403() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Access Denied");
    }

    @Test
    void handleUserNotFound_returns404() {
        ProblemDetail problem = handler.handleUserNotFound(new UserNotFoundException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("User Not Found");
    }

    @Test
    void handleUserNotFound_messageConstructor_returns404() {
        ProblemDetail problem = handler.handleUserNotFound(new UserNotFoundException("No user found for keycloakId: kc-1"));

        assertThat(problem.getDetail()).isEqualTo("No user found for keycloakId: kc-1");
    }

    @Test
    void handleDuplicateEmail_returns409() {
        ProblemDetail problem = handler.handleDuplicateEmail(new DuplicateEmailException("mario@example.com"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Duplicate Email");
    }

    @Test
    void handleInvalidUserOperation_returns422() {
        ProblemDetail problem = handler.handleInvalidUserOperation(new InvalidUserOperationException("not allowed"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid User Operation");
    }

    @Test
    void handleMethodArgumentNotValid_collectsFieldErrors_andFallsBackWhenMessageIsNull() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "must not be blank"),
                new FieldError("request", "role", null)
        ));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleMethodArgumentNotValid(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("email", "must not be blank");
        assertThat(errors).containsEntry("role", "Invalid value");
    }

    @Test
    void handleConstraintViolation_returns400WithViolations() {
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("skill");
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException("invalid parameters", Set.of(violation));

        ProblemDetail problem = handler.handleConstraintViolation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("skill", "must not be blank");
    }

    @Test
    void handleMissingServletRequestParameter_returns400() {
        ProblemDetail problem = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("skill", "String"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Missing Parameter");
    }

    @Test
    void handleGenericException_returns500() {
        ProblemDetail problem = handler.handleGenericException(new RuntimeException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred. Please contact support.");
    }
}

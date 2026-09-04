package com.skillmatch.projectservice.exception;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for every handler in {@link GlobalExceptionHandler}, exercised without a
 * Spring context. Some domain exception paths are already covered indirectly by the controller
 * test slices; this class fills in the handlers that are otherwise never triggered (constraint
 * violations, missing parameters, the generic fallback) and re-asserts the rest in isolation.
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
    void handleProjectNotFound_returns404() {
        ProblemDetail problem = handler.handleProjectNotFound(new ProjectNotFoundException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Project Not Found");
    }

    @Test
    void handleCandidatureNotFound_returns404() {
        ProblemDetail problem = handler.handleCandidatureNotFound(new CandidatureNotFoundException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Candidature Not Found");
    }

    @Test
    void handleInvalidProjectOperation_returns422() {
        ProblemDetail problem = handler.handleInvalidProjectOperation(new InvalidProjectOperationException("not allowed"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Project Operation");
    }

    @Test
    void handleUserServiceUnavailable_returns503() {
        ProblemDetail problem = handler.handleUserServiceUnavailable(
                new UserServiceUnavailableException("down", new RuntimeException("timeout")));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("User Service Unavailable");
    }

    @Test
    void handleMethodArgumentNotValid_collectsFieldErrors_andFallsBackWhenMessageIsNull() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "title", "must not be blank"),
                new FieldError("request", "budget", null)
        ));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleMethodArgumentNotValid(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("title", "must not be blank");
        assertThat(errors).containsEntry("budget", "Invalid value");
    }

    @Test
    void handleConstraintViolation_returns400WithViolations() {
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("projectId");
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must be a valid UUID");

        ConstraintViolationException ex = new ConstraintViolationException("invalid parameters", Set.of(violation));

        ProblemDetail problem = handler.handleConstraintViolation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("projectId", "must be a valid UUID");
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

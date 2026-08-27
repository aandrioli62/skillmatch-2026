package com.skillmatch.contractservice.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler producing RFC 7807 Problem Detail responses.
 * Spring Boot 3.x natively supports {@link ProblemDetail} as the response body.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://skillmatch.io/problems/";

    // -------------------------------------------------------------------------
    // Domain exceptions
    // -------------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "access-denied"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ContractNotFoundException.class)
    public ProblemDetail handleContractNotFound(ContractNotFoundException ex) {
        log.warn("Contract not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "contract-not-found"));
        problem.setTitle("Contract Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidContractOperationException.class)
    public ProblemDetail handleInvalidContractOperation(InvalidContractOperationException ex) {
        log.warn("Invalid contract operation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "invalid-contract-operation"));
        problem.setTitle("Invalid Contract Operation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ProblemDetail handleUserServiceUnavailable(UserServiceUnavailableException ex) {
        log.error("User Service unavailable: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "user-service-unavailable"));
        problem.setTitle("User Service Unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // -------------------------------------------------------------------------
    // Validation exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles @Valid failures on @RequestBody parameters.
     * Collects per-field validation errors and embeds them in the Problem Detail.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first   // keep first error per field
                ));

        log.warn("Validation failed: {}", fieldErrors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body contains invalid fields. See 'errors' for details.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("errors", fieldErrors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles @Validated constraint violations on path/query params.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (first, second) -> first
                ));

        log.warn("Constraint violation: {}", violations);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request parameters are invalid. See 'errors' for details.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("errors", violations);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles missing required @RequestParam values.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "missing-parameter"));
        problem.setTitle("Missing Parameter");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // -------------------------------------------------------------------------
    // Fallback
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}

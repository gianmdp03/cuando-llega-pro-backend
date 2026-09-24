package com.gianmdp03.cuando_llega_pro.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.net.URI;
import java.util.List;

/**
 * Centralized global exception handler mapping domain and framework exceptions
 * to RFC 7807 ProblemDetail representations.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .toList();

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Invalid request payload"
        );
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ProblemDetail handleUpstreamService(UpstreamServiceException ex) {
        log.error("Upstream proxy failure: {}", ex.getMessage(), ex);
        return createProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "Upstream Proxy Failure",
                ex.getMessage()
        );
    }

    @ExceptionHandler(CatalogRefreshRequiredException.class)
    public ProblemDetail handleCatalogRefreshRequired(CatalogRefreshRequiredException ex) {
        ProblemDetail problem = createProblemDetail(
                HttpStatus.CONFLICT,
                "MGP Client Refresh Required",
                ex.getMessage()
        );
        problem.setType(URI.create("urn:cuando-llega:mgp-client-refresh-required"));
        return problem;
    }

    @ExceptionHandler(TelemetryRefreshRequiredException.class)
    public ProblemDetail handleTelemetryRefreshRequired(TelemetryRefreshRequiredException ex) {
        ProblemDetail problem = createProblemDetail(
                HttpStatus.CONFLICT,
                "MGP Client Refresh Required",
                ex.getMessage()
        );
        problem.setType(URI.create("urn:cuando-llega:mgp-client-refresh-required"));
        return problem;
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return createProblemDetail(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                ex.getMessage()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return createProblemDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please contact support."
        );
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}

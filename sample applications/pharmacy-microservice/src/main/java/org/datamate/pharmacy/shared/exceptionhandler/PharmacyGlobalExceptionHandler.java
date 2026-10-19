package org.datamate.pharmacy.shared.exceptionhandler;

import com.datamate.bedrock.framework.common.exception.config.ExceptionProperties;
import com.datamate.bedrock.framework.common.exception.service.MessageResolver;
import com.datamate.bedrock.framework.common.exception.spring.service.web.GlobalExceptionHandler;
import org.datamate.authz.exception.AuthzDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Pharmacy microservice.
 * Extends Bedrock's GlobalExceptionHandler with enriched RFC 7807 ProblemDetail responses.
 * Aligned with the dental project's DentistGlobalExceptionHandler.
 */
@RestControllerAdvice
public class PharmacyGlobalExceptionHandler extends GlobalExceptionHandler {

    public PharmacyGlobalExceptionHandler(MessageResolver resolver, ExceptionProperties properties) {
        super(resolver, properties);
    }

    @EnableLogger
    private Logger logger;

    // ──────────────────────────────────────────────
    // Enrichment helper — adds standard properties to every ProblemDetail
    // ──────────────────────────────────────────────
    private ProblemDetail enrich(ProblemDetail pd, HttpServletRequest request) {
        pd.setProperty("errorCode", pd.getTitle());
        pd.setProperty("timestamp", Instant.now().toString());
        if (request != null) {
            pd.setProperty("path", request.getRequestURI());
            pd.setProperty("method", request.getMethod());
        }
        return pd;
    }

    // ──────────────────────────────────────────────
    // OPA Authorization exceptions
    // ──────────────────────────────────────────────

    @ExceptionHandler(AuthzDeniedException.class)
    public ProblemDetail handleAuthzDeniedException(AuthzDeniedException ex, HttpServletRequest request) {
        String title = "ACCESS_DENIED";
        String detail = ex.getMessage();

        if (detail != null && detail.contains("|")) {
            String[] parts = detail.split("\\|", 2);
            title = parts[0];
            detail = parts[1];
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        pd.setTitle(title);
        return enrich(pd, request);
    }

    @ExceptionHandler(org.datamate.authz.exception.AuthzInvalidSyntaxException.class)
    public ProblemDetail handleAuthzInvalidSyntaxException(
            org.datamate.authz.exception.AuthzInvalidSyntaxException ex, HttpServletRequest request) {
        logger.warn("Invalid syntax in policy payload: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("COMPILATION_ERROR");
        return enrich(pd, request);
    }

    @ExceptionHandler(org.datamate.authz.exception.AuthzInvalidPayloadException.class)
    public ProblemDetail handleAuthzInvalidPayloadException(
            org.datamate.authz.exception.AuthzInvalidPayloadException ex, HttpServletRequest request) {
        logger.warn("Invalid payload in policy: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("INVALID_PAYLOAD");
        return enrich(pd, request);
    }

    // ──────────────────────────────────────────────
    // Spring Security exceptions
    // ──────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("FORBIDDEN");
        return enrich(pd, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("UNAUTHORIZED");
        return enrich(pd, request);
    }

    // ──────────────────────────────────────────────
    // Validation exceptions
    // ──────────────────────────────────────────────



    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("CONSTRAINT_VIOLATION");
        return enrich(pd, request);
    }

    // ──────────────────────────────────────────────
    // Concurrency exceptions
    // ──────────────────────────────────────────────

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource was modified by another user. Please refresh and try again."
        );
        pd.setTitle("CONCURRENT_MODIFICATION");
        return enrich(pd, request);
    }

    // ──────────────────────────────────────────────
    // Generic fallbacks
    // ──────────────────────────────────────────────

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        logger.warn("Resource not found: {}", ex.getResourcePath());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested endpoint does not exist.");
        pd.setTitle("NOT_FOUND");
        return enrich(pd, request);
    }


}

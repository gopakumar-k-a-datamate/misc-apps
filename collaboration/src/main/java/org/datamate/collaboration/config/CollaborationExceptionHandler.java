package org.datamate.collaboration.config;

import com.datamate.bedrock.framework.common.exception.config.ExceptionProperties;
import com.datamate.bedrock.framework.common.exception.service.MessageResolver;
import com.datamate.bedrock.framework.common.exception.spring.service.web.GlobalExceptionHandler;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationBaseException;
import org.datamate.collaboration.exception.DocumentConversionException;
import org.datamate.collaboration.exception.DomainValidationException;
import org.datamate.collaboration.exception.ResourceNotFoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.OffsetDateTime;

/**
 * Global Exception Handler for the Collaboration Microservice.
 */
@RestControllerAdvice
public class CollaborationExceptionHandler extends GlobalExceptionHandler {

    @EnableLogger
    private Logger logger;
    
    private final MessageResolver messageResolver;
    private final ResourceBundleMessageSource fallbackMessageSource;

    public CollaborationExceptionHandler(MessageResolver resolver, ExceptionProperties properties) {
        super(resolver, properties);
        this.messageResolver = resolver;
        this.fallbackMessageSource = new ResourceBundleMessageSource();
        this.fallbackMessageSource.setBasename("messages");
        this.fallbackMessageSource.setDefaultEncoding("UTF-8");
        this.fallbackMessageSource.setUseCodeAsDefaultMessage(true);
    }

    @PostConstruct
    public void initSuperLogger() {
        try {
            Field superLoggerField = GlobalExceptionHandler.class.getDeclaredField("logger");
            superLoggerField.setAccessible(true);
            superLoggerField.set(this, this.logger);
        } catch (Exception ignored) {
            // Ignored if field structure changes in future versions
        }
    }
    
    private String resolveMessage(CollaborationBaseException ex) {
        String customMsg = ex.getCustomMessage();
        if (customMsg != null && !customMsg.isBlank()) {
            return customMsg;
        }
        String resolved = messageResolver.resolveMessage(ex.getErrorCode(), LocaleContextHolder.getLocale(), ex.getMessageArgs());
        if (resolved == null || resolved.equals(ex.getErrorCode())) {
             resolved = fallbackMessageSource.getMessage(ex.getErrorCode(), ex.getMessageArgs(), ex.getErrorCode(), LocaleContextHolder.getLocale());
        }
        return resolved != null ? resolved : ex.getErrorCode();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        String msg = resolveMessage(ex);
        if (logger != null) {
            logger.warn("Resource not found [errorCode: {}]: {}", ex.getErrorCode(), msg);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, msg);
        enrich(pd, ex);
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        if (logger != null) {
            logger.warn("Endpoint or static resource not found: {}", request.getRequestURI());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Resource Not Found");
        pd.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }


    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            if (logger != null) {
                logger.warn("Unauthorized request on [{}]: {}", request.getRequestURI(), ex.getMessage());
            }
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or missing authentication ticket");
            pd.setType(URI.create("about:blank"));
            pd.setTitle("Unauthorized");
            pd.setProperty("errorCode", "AUTH_UNAUTHORIZED");
            pd.setProperty("timestamp", OffsetDateTime.now().toString());
            return pd;
        }

        if (logger != null) {
            logger.warn("Access denied on [{}]: {}", request.getRequestURI(), ex.getMessage());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Forbidden");
        pd.setProperty("errorCode", "AUTH_FORBIDDEN");
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleAuthentication(org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        if (logger != null) {
            logger.warn("Authentication failed on [{}]: {}", request.getRequestURI(), ex.getMessage());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or missing authentication ticket");
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Unauthorized");
        pd.setProperty("errorCode", "AUTH_UNAUTHORIZED");
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        String msg = resolveMessage(ex);
        if (logger != null) {
            logger.warn("Domain validation failed [errorCode: {}]: {}", ex.getErrorCode(), msg);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        enrich(pd, ex);
        return pd;
    }

    @ExceptionHandler(ApplicationValidationException.class)
    public ProblemDetail handleApplicationValidation(ApplicationValidationException ex) {
        String msg = resolveMessage(ex);
        if (logger != null) {
            logger.warn("Application validation failed [errorCode: {}]: {}", ex.getErrorCode(), msg);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        enrich(pd, ex);
        return pd;
    }

    @ExceptionHandler(DocumentConversionException.class)
    public ProblemDetail handleDocumentConversion(DocumentConversionException ex) {
        String msg = resolveMessage(ex);
        if (logger != null) {
            logger.error("Document conversion error [errorCode: {}]: {}", ex.getErrorCode(), msg);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, msg);
        enrich(pd, ex);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    @Override
    public ProblemDetail handleAny(Exception ex, HttpServletRequest request) {
        if (logger != null) {
            logger.error("Unexpected exception occurred on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred");
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Internal Server Error");
        pd.setProperty("errorCode", "COMMON_INTERNAL");
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }

    /**
     * Enriches a ProblemDetail with standard Collaboration error metadata.
     */
    private void enrich(ProblemDetail pd, CollaborationBaseException ex) {
        pd.setType(URI.create("about:blank"));
        pd.setTitle(ex.getErrorCode());
        pd.setProperty("errorCode", ex.getErrorCode());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
    }
}

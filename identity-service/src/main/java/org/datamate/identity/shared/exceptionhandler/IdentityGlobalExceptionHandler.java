package org.datamate.identity.shared.exceptionhandler;

import com.datamate.bedrock.framework.common.exception.config.ExceptionProperties;
import com.datamate.bedrock.framework.common.exception.exceptions.BaseAppException;
import com.datamate.bedrock.framework.common.exception.service.MessageResolver;
import com.datamate.bedrock.framework.common.exception.spring.service.web.GlobalExceptionHandler;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.BedrockMDC;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.datamate.bedrock.framework.common.logging.util.LoggerManager;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * IdentityGlobalExceptionHandler - Application-Level Exception Handler
 *
 * ARCHITECTURE:
 * - Extends framework's GlobalExceptionHandler
 * - Customizes error handling for OPA identity-service
 * - Integrates with i18n for message resolution
 * - Maps error codes to HTTP status codes
 */
@RestControllerAdvice(basePackages = "org.datamate.identity")
public class IdentityGlobalExceptionHandler extends GlobalExceptionHandler {

    @EnableLogger
    private Logger log;

    private final MessageSource messageSource;
    private static final String TRACE_ID_KEY = "traceId";

    public IdentityGlobalExceptionHandler(
            MessageResolver resolver,
            ExceptionProperties properties,
            MessageSource messageSource
    ) {
        super(resolver, properties);
        this.messageSource = messageSource;

        try {
            Logger classLogger = LoggerManager.getLogger(this.getClass());
            this.log = classLogger;

            Field parentLoggerField = GlobalExceptionHandler.class.getDeclaredField("logger");
            parentLoggerField.setAccessible(true);
            parentLoggerField.set(this, classLogger);
        } catch (Exception e) {
            // Fallback: If LoggerManager isn't initialized yet, rely on Spring's post processor
        }
    }

    @Override
    @ExceptionHandler(BaseAppException.class)
    public ProblemDetail handleBase(BaseAppException ex, HttpServletRequest request) {

        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY))
                .orElse("unknown");

        log.error("BaseAppException occurred [traceId: {}, errorCode: {}]: {}",
                traceId, ex.getErrorCode(), ex.getMessage(), ex);

        Locale locale = LocaleContextHolder.getLocale();
        String errorCode = ex.getErrorCode();

        // Resolve message from i18n with parameters
        String resolvedMessage = resolveMessage(errorCode, locale, ex.getMessageArgs());

        String detail;
        if (resolvedMessage != null && !resolvedMessage.isBlank()) {
            detail = resolvedMessage;
        } else if (ex.getCustomMessage() != null && !ex.getCustomMessage().isBlank() && !ex.getCustomMessage().equals(errorCode)) {
            detail = ex.getCustomMessage();
        } else {
            detail = errorCode;
        }

        // Map error code to HTTP status
        HttpStatus status = determineHttpStatus(errorCode);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        enrichProblemDetail(pd, errorCode, request);

        return pd;
    }

    @Override
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {

        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY))
                .orElse("unknown");

        Locale locale = LocaleContextHolder.getLocale();
        String errorCode = "VALIDATION_ERROR";

        if (ex instanceof MethodArgumentNotValidException validationEx) {

            log.warn("Validation error occurred [traceId: {}]: {}", traceId, validationEx.getMessage());

            String detail = validationEx.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> resolveFieldErrorMessage(error, locale))
                    .findFirst()
                    .orElseGet(() -> resolveMessage("validation.failed", locale, null));

            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    detail
            );

            enrichProblemDetail(pd, errorCode, request);

            return pd;
        }

        if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {

            log.warn("Type mismatch [traceId: {}]: {}", traceId, mismatchEx.getMessage());

            String paramName = mismatchEx.getName();
            Object invalidValue = mismatchEx.getValue();

            String detail = resolveMessage("validation.invalid.parameter", locale, new Object[]{paramName, invalidValue});

            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
            enrichProblemDetail(pd, errorCode, request);
            return pd;
        }

        // handle other bad requests
        log.warn("Bad request [traceId: {}]: {}", traceId, ex.getMessage());

        String detail = resolveDetailFromExceptionMessage(ex.getMessage(), locale, "Bad request");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        enrichProblemDetail(pd, errorCode, request);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY)).orElse("unknown");

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof BaseAppException baseAppEx) {
                return handleBase(baseAppEx, request);
            }
            if (cause instanceof InvalidFormatException invalidFormatEx) {
                if (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum()) {
                    String fieldName = invalidFormatEx.getPath().stream()
                            .map(JsonMappingException.Reference::getFieldName)
                            .filter(Objects::nonNull)
                            .reduce((first, second) -> second)
                            .orElse("field");
                    String detail = String.format("Invalid value '%s' for field '%s'. Accepted values are: %s",
                            invalidFormatEx.getValue(),
                            fieldName,
                            Arrays.toString(invalidFormatEx.getTargetType().getEnumConstants()));

                    log.warn("Invalid enum value [traceId: {}]: {}", traceId, detail);
                    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
                    enrichProblemDetail(pd, "INVALID_VALUE", request);
                    return pd;
                }
            }
            cause = cause.getCause();
        }

        // Log the specific deserialization failure
        log.warn("HttpMessageNotReadableException occurred [traceId: {}]: {}", traceId, ex.getMessage());

        String detail = "The request body is unreadable or malformed.";
        String errorCode = "JSON_PARSE_ERROR";

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        enrichProblemDetail(pd, errorCode, request);

        return pd;
    }

    private String resolveMessage(String code, Locale locale, Object[] args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Could not resolve message for code: {}", code);
            return null;
        }
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }

        // NOT FOUND patterns
        if (errorCode.endsWith(".notFound")
                || errorCode.contains(".NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        }

        // LOCKED patterns (concurrent edit lock — RFC 4918)
        if (errorCode.contains(".lock.lockedByOther")) {
            return HttpStatus.LOCKED;   // 423
        }

        // CONFLICT patterns (already exists, or lock not owned)
        if (errorCode.endsWith(".alreadyExists")
                || errorCode.endsWith(".lock.notOwner")
                || errorCode.contains(".ALREADY_EXISTS")
                || errorCode.contains(".CONFLICT")) {
            return HttpStatus.CONFLICT;
        }

        // FORBIDDEN patterns
        if (errorCode.contains(".FORBIDDEN")
                || errorCode.contains(".ACCESS_DENIED")) {
            return HttpStatus.FORBIDDEN;
        }

        // UNAUTHORIZED patterns
        if (errorCode.contains(".UNAUTHORIZED")
                || errorCode.contains(".AUTHENTICATION")) {
            return HttpStatus.UNAUTHORIZED;
        }

        // VALIDATION patterns
        if (errorCode.contains(".validation")
                || errorCode.contains(".invalid")) {
            return HttpStatus.BAD_REQUEST;
        }

        // Default
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY))
                .orElse("unknown");

        log.error("Access denied [traceId: {}]: {}", traceId, ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();

        String detail = resolveDetailFromExceptionMessage(ex.getMessage(), locale, "Access is denied. You do not have sufficient permissions.");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                detail);

        enrichProblemDetail(pd, "SECURITY.ACCESS_DENIED", request);
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY))
                .orElse("unknown");

        log.error("Authentication failed [traceId: {}]: {}", traceId, ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();

        String detail = resolveDetailFromExceptionMessage(ex.getMessage(), locale, "Authentication failed. Please provide valid credentials.");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                detail);

        enrichProblemDetail(pd, "SECURITY.UNAUTHORIZED", request);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAny(Exception ex, HttpServletRequest request) {
        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY))
                .orElse("unknown");

        log.error("Unexpected exception occurred [traceId: {}]: {}", traceId, ex.getMessage(), ex);
        Locale locale = LocaleContextHolder.getLocale();

        String detail = resolveDetailFromExceptionMessage(ex.getMessage(), locale, "An unexpected internal error occurred.");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                detail);

        enrichProblemDetail(pd, "INTERNAL_SERVER_ERROR", request);
        return pd;
    }

    private void enrichProblemDetail(ProblemDetail pd, String errorCode,
            HttpServletRequest req) {
        pd.setTitle(errorCode);
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("timestamp", OffsetDateTime.now().toString());

        if (req != null) {
            pd.setProperty("path", req.getRequestURI());
            pd.setProperty("method", req.getMethod());
        }

        pd.setProperty("traceId",
                Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY)).orElse("unknown"));
    }

    private String resolveFieldErrorMessage(FieldError error, Locale locale) {
        String messageCode = error.getDefaultMessage();
        if (messageCode != null) {
            String resolvedMessage = resolveMessage(messageCode, locale, error.getArguments());
            if (resolvedMessage != null && !resolvedMessage.isBlank()) {
                return resolvedMessage;
            }
            return messageCode;
        }
        return "Validation failed for field: " + error.getField();
    }

    private String resolveDetailFromExceptionMessage(String exceptionMessage, Locale locale, String fallbackMessage) {
        if (isMessageCode(exceptionMessage)) {
            String resolved = resolveMessage(exceptionMessage, locale, null);
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
            return exceptionMessage;
        }
        return exceptionMessage != null && !exceptionMessage.isBlank() ? exceptionMessage : fallbackMessage;
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockingFailureException.class
    })
    public ProblemDetail handleOptimisticLocking(Exception ex, HttpServletRequest request) {
        String traceId = Optional.ofNullable(BedrockMDC.get(TRACE_ID_KEY)).orElse("unknown");
        log.error("Optimistic locking conflict occurred [traceId: {}]: {}", traceId, ex.getMessage(), ex);

        Locale locale = LocaleContextHolder.getLocale();
        String errorCode = "concurrent.modification";
        String detail = resolveMessage(errorCode, locale, null);
        if (detail == null || detail.isBlank()) {
            detail = "Resource modified by another user please re-try again.";
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        enrichProblemDetail(pd, errorCode, request);
        return pd;
    }

    private boolean isMessageCode(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return !message.trim().contains(" ");
    }
}

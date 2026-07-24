package com.digitalheroes.urlaudit.exception;

import com.digitalheroes.urlaudit.dto.ApiResponse;
import com.digitalheroes.urlaudit.util.RequestIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        AuditErrorCode errorCode = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "url".equals(error.getField()))
                ? AuditErrorCode.INVALID_URL
                : AuditErrorCode.INVALID_REQUEST;
        return error(HttpStatus.BAD_REQUEST, errorCode.name(), message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse("Request validation failed");
        String code = exception.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("url"))
                ? AuditErrorCode.INVALID_URL.name()
                : AuditErrorCode.INVALID_REQUEST.name();
        return error(HttpStatus.BAD_REQUEST, code, message, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                AuditErrorCode.INVALID_REQUEST.name(),
                "Request could not be processed",
                request);
    }

    @ExceptionHandler(UrlAuditException.class)
    public ResponseEntity<ApiResponse<Void>> handleUrlAuditException(
            UrlAuditException exception,
            HttpServletRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                AuditErrorCode.UNEXPECTED_ERROR.name(),
                "An unexpected error occurred",
                request,
                exception);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return error(status, code, message, request, null);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Throwable cause) {
        String requestId = RequestIdUtils.from(request);
        logError(status, code, requestId, cause);
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, requestId));
    }

    private void logError(
            HttpStatus status,
            String code,
            String requestId,
            Throwable cause) {
        if (status.is5xxServerError()) {
            var event = log.atError()
                    .addKeyValue("event", "api_error")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("errorCode", code)
                    .addKeyValue("status", status.value());
            if (cause != null) {
                event.setCause(cause);
            }
            event.log("api_error");
            return;
        }

        log.atWarn()
                .addKeyValue("event", "api_error")
                .addKeyValue("requestId", requestId)
                .addKeyValue("errorCode", code)
                .addKeyValue("status", status.value())
                .log("api_error");
    }
}

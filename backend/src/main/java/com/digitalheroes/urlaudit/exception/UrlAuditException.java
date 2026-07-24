package com.digitalheroes.urlaudit.exception;

import org.springframework.http.HttpStatus;

public class UrlAuditException extends RuntimeException {

    private final AuditErrorCode code;
    private final HttpStatus status;

    public UrlAuditException(AuditErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public UrlAuditException(AuditErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code.name();
    }

    public HttpStatus getStatus() {
        return status;
    }
}

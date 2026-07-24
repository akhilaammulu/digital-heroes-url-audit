package com.digitalheroes.urlaudit.exception;

public enum AuditErrorCode {
    INVALID_REQUEST,
    INVALID_URL,
    TIMEOUT,
    DNS_FAILURE,
    CONNECTION_FAILURE,
    UNEXPECTED_ERROR,
    RATE_LIMIT_EXCEEDED,
    CONCURRENCY_LIMIT_EXCEEDED
}

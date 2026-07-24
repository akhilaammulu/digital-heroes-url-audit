package com.digitalheroes.urlaudit.dto;

import java.time.Instant;

public record AuditResponse(
        String url,
        int httpStatus,
        long responseTimeMs,
        String pageTitle,
        Instant timestamp) {
}

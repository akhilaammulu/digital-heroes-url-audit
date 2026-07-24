package com.digitalheroes.urlaudit.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "url-audit")
public record UrlAuditProperties(
        Duration requestTimeout,
        Duration cacheTtl,
        long cacheMaximumSize) {

    public UrlAuditProperties {
        validatePositive("requestTimeout", requestTimeout);
        validatePositive("cacheTtl", cacheTtl);
        if (cacheMaximumSize <= 0) {
            throw new IllegalArgumentException("cacheMaximumSize must be greater than zero");
        }
    }

    private static void validatePositive(String propertyName, Duration value) {
        Objects.requireNonNull(value, propertyName + " must be configured");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
    }
}

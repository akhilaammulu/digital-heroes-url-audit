package com.digitalheroes.urlaudit.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "url-audit")
public record UrlAuditProperties(Duration requestTimeout, Duration cacheTtl) {

    public UrlAuditProperties {
        validatePositive("requestTimeout", requestTimeout);
        validatePositive("cacheTtl", cacheTtl);
    }

    private static void validatePositive(String propertyName, Duration value) {
        Objects.requireNonNull(value, propertyName + " must be configured");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
    }
}

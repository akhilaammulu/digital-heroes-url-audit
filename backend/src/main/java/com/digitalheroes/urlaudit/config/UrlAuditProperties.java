package com.digitalheroes.urlaudit.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "url-audit")
public record UrlAuditProperties(
        Duration requestTimeout,
        Duration cacheTtl,
        long cacheMaximumSize,
        RateLimit rateLimit,
        Concurrency concurrency) {

    public record RateLimit(
            int capacity,
            int refillTokens,
            Duration duration) {
        public RateLimit {
            if (capacity <= 0) {
                throw new IllegalArgumentException("rateLimit.capacity must be greater than zero");
            }
            if (refillTokens <= 0) {
                throw new IllegalArgumentException("rateLimit.refillTokens must be greater than zero");
            }
            Objects.requireNonNull(duration, "rateLimit.duration must be configured");
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("rateLimit.duration must be greater than zero");
            }
        }
    }

    public record Concurrency(
            int maxConcurrentAudits) {
        public Concurrency {
            if (maxConcurrentAudits <= 0) {
                throw new IllegalArgumentException("concurrency.maxConcurrentAudits must be greater than zero");
            }
        }
    }


    public UrlAuditProperties {
        validatePositive("requestTimeout", requestTimeout);
        validatePositive("cacheTtl", cacheTtl);
        if (cacheMaximumSize <= 0) {
            throw new IllegalArgumentException("cacheMaximumSize must be greater than zero");
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(100, 100, Duration.ofMinutes(1));
        }
        if (concurrency == null) {
            concurrency = new Concurrency(10);
        }
    }

    private static void validatePositive(String propertyName, Duration value) {
        Objects.requireNonNull(value, propertyName + " must be configured");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
    }
}

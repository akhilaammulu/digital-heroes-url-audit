# Observability

This document details the observability, request tracing, logging patterns, health check configurations, and exception mappings implemented in the URL Audit Service.

---

## 1. Request Tracing & Correlation ID Propagation

To trace individual execution flows across independent filters, caching layers, services, and outbound requests, the system implements a correlation ID mechanism:

1. **`RequestIdFilter.java`**:
   * Intercepts all incoming HTTP requests.
   * Extracts the **`X-Request-Id`** header from the request.
   * If the header is missing, it automatically generates a unique correlation ID (`UUID.randomUUID().toString()`).
   * Binds the correlation ID to the logging thread context using **SLF4J MDC** under the key `requestId`.
   * Sets the `X-Request-Id` response header to return the correlation ID back to the client.
   * Ensures the MDC context is cleared in a `finally` block to prevent thread pool leakage.

---

## 2. Structured Logging Pattern

The console logging configuration format includes the correlation ID in every request-scoped line:

```text
logging:
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%X{requestId:-no-request-id}] %logger{36} - %msg%n"
```

### Log Line Example
```text
2026-07-25T13:02:48.559+05:30 INFO  [ed958fc8-f062-438d-928a-d72ec01b91ed] c.d.urlaudit.config.RequestIdFilter - audit_request_received
```

### Standardized Log Events
To support log aggregation systems (e.g., Splunk, ELK, Grafana Loki), the service uses strict, grep-friendly string tags:

| Event Tag | Log Level | Location | Purpose |
| :--- | :--- | :--- | :--- |
| `audit_request_received` | `INFO` | `RequestIdFilter.java` | Logged when a request enters the server. |
| `audit_request_completed` | `INFO` | `RequestIdFilter.java` | Logged when a request is returning to the client. |
| `audit_started` | `INFO` | `AuditServiceImpl.java` | Logged before making the WebClient outbound call. |
| `audit_response_received` | `INFO` | `AuditServiceImpl.java` | Logged when WebClient receives a response from the target website. |
| `cache_hit` | `INFO` | `LoggingCaffeineCache.java` | Logged when an audit resolves via local Cache. |
| `cache_miss` | `INFO` | `LoggingCaffeineCache.java` | Logged when the cache doesn't contain the URL. |
| `api_error` | `ERROR` | `GlobalExceptionHandler.java` | Logged when an unhandled runtime error is caught. |

---

## 3. Health & Readiness/Liveness Probes

The backend exposes Spring Boot Actuator probes at `/actuator/health`:

* **`liveness`**: Determines if the JVM container is running. If it reports `DOWN`, the orchestrator (e.g. Kubernetes, Render) will restart the container.
* **`readiness`**: Determines if the application is ready to process traffic. If it reports `DOWN`, the load balancer stops routing traffic to the instance.

```yaml
management:
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState
```

---

## 4. Structured Error Handling

Exceptions are centralized using `@RestControllerAdvice` in `GlobalExceptionHandler.java`:

1. **`UrlAuditException`**: Business exceptions containing specific error codes from `AuditErrorCode.java` (e.g., `TIMEOUT`, `DNS_FAILURE`, `CONNECTION_FAILURE`, `CONCURRENCY_LIMIT_EXCEEDED`).
2. **`MethodArgumentNotValidException`**: Handled to catch `@NotBlank` or `@Pattern` failures on request DTO parameters and return `INVALID_REQUEST`.
3. **Unexpected Throwables**: Caught at the root level, logged at `ERROR` level as `api_error` with full stack traces, and mapped to `UNEXPECTED_ERROR` (HTTP 500) to keep the client response clean.

---

## 5. Monitoring & Future Improvements

To enhance production monitoring in the future, the following strategies are recommended:

* **Prometheus Metrics**: Integrate Micrometer to export metrics for Prometheus scraping (e.g., active request counters, audit durations, cache hit ratio, and rate limiting drops).
* **JSON Log Formatting**: Swap standard console log strings with a JSON formatter (like Logstash Logback Encoder) to allow log ingestion engines to natively parse MDC keys.
* **Distributed Tracing**: Integrate OpenTelemetry to automatically trace the audit request from the browser client, through the backend service, all the way to target site responses.

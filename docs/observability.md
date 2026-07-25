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

---

## 6. Alerting & Rollback Strategy

### 6.1 Metrics to Monitor & Alert On
To maintain a high-quality SLA, we configure alerts on the following key metrics:
1. **HTTP 5xx Error Rate**: Trigger P1 alert if > 1% of total requests return 5xx (500, 502, 503, 504) over a rolling 5-minute window.
2. **Concurrency Limit Saturation (HTTP 503)**: Alert if 503 rejections exceed 5% of traffic, signaling the need for horizontal scaling.
3. **Client-IP Rate Limiter Drops (HTTP 429)**: Monitor 429 spike alerts to detect brute-force scans or DDoS attempts.
4. **Endpoint Latency**: Alert if the 95th percentile (p95) response latency of successful audits exceeds 2 seconds.
5. **JVM Memory/CPU Utilization**: Trigger warning if container heap memory usage exceeds 85% or CPU exceeds 80% for > 3 minutes.

### 6.2 Deployment Rollback Plan
To recover rapidly from a faulty release:
1. **Automated Zero-Downtime Rollback**:
   * Render utilizes a rolling deployment strategy. During a release, the new container starts, and Render repeatedly checks the readiness probe (`/actuator/health/readiness`).
   * If the container fails to start, crashes, or the readiness probe fails to return HTTP 200 within 5 minutes, Render aborts the deployment and routes 100% of traffic to the active, stable instances.
2. **Manual Rollback**:
   * If a bug escapes integration tests and is discovered post-release:
     1. Open the **Render Dashboard** and select the service.
     2. Click on the **Deploys** tab.
     3. Locate the last stable deployment corresponding to the verified Git hash.
     4. Click the options menu next to it and select **Rollback to this deploy**. This instantly routes traffic to the stable image version.


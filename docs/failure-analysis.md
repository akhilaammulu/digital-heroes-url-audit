# Failure Analysis

This document analyzes how the URL Audit Service handles various failure modes and edge cases, detailing the expected HTTP responses, user interface experiences, logging actions, and recovery procedures for each scenario.

---

## 1. Scenario Matrix

| Failure / Scenario | Expected HTTP Code | User Experience | Logging Behavior | Recovery Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **Invalid URL Submitted** | `400 Bad Request` | Form shows a red validation error helper message. Submission is blocked. | Validation warning in logs with MDC correlation ID (if bypassed to backend). | Enter a valid URL starting with `http://` or `https://`. |
| **DNS Lookup Fails** | `502 Bad Gateway` | Red Error Card: *"Connection Failure: DNS resolution failed..."* | Warn log containing target URL and MDC correlation ID. | Check spelling of target domain or check target domain registrar. |
| **Remote Server 404** | `200 OK` (Successful Audit) | Green Completed Card displaying a red badge: *"HTTP 404 - Error"*. | Standard request lifecycle logs. | Verify the specific page path on the target domain. |
| **Remote Server Times Out** | `504 Gateway Timeout` | Red Error Card: *"Timeout: The request timed out (limit: 10s)"*. | Timeout exception caught and logged as warn trace in service layer. | Try again later or increase connection timeout dynamic property limits. |
| **Rate Limit Exceeded** | `429 Too Many Requests` | Red Error Card: *"Rate Limit Exceeded (HTTP 429)"*. | Filter logs IP address blocking events. | Wait for the 1-minute window to slide, refilling tokens. |
| **Max Concurrent Audits Reached** | `503 Service Unavailable` | Red Error Card: *"Concurrency Limit Exceeded (HTTP 503)"*. | Audit service logs semaphore block. | Wait a few seconds for active audits to release semaphore threads. |
| **Backend Unavailable** | Browser network error | Red Error Card: *"Could not connect to the audit service..."* | Client console network error (no backend logs). | Check if backend service has crashed or is waking up from free-tier sleep. |
| **Cache Miss** | `200 OK` | Normal loading spinner for a few hundred milliseconds while target is fetched. | `cache_miss` logged. Service fetches target and writes to cache. | Automatic. |
| **Cache Hit** | `200 OK` | Instant response (0 ms load time) upon form submission. | `cache_hit` logged. Outbound WebClient call skipped. | Automatic. Cache expires after configured TTL (default 5 minutes). |

---

## 2. Detailed Scenario Breakdown

### 2.1 Invalid URL Submitted
* **System Action**: 
  * The frontend client-side validation schema (Zod) blocks form submission if the URL is empty or does not start with `http://` or `https://`.
  * If the request is sent directly to the API, JSR-380 validation catches it.
* **HTTP Code**: `400 Bad Request` (payload: `{"success":false,"error":{"code":"INVALID_REQUEST","message":"..."}}`).
* **Logs**: Backend triggers warning level logs inside `GlobalExceptionHandler.java`. No exception stack trace is logged.
* **Recovery**: Client must correct the input field format.

### 2.2 DNS Lookup Fails
* **System Action**: WebClient throws a `NameResolverException` / `UnknownHostException` when executing the HTTP GET.
* **HTTP Code**: `502 Bad Gateway` (payload: `{"success":false,"error":{"code":"DNS_FAILURE","message":"..."}}`).
* **Logs**: `GlobalExceptionHandler` maps the exception and prints:
  `WARN  [requestId] com.digitalheroes.urlaudit.exception.GlobalExceptionHandler - api_error: DNS resolution failed`
* **Recovery**: Verify target URL spelling or check target server network connectivity.

### 2.3 Remote Server Returns 404
* **System Action**: The audit service successfully establishes a connection with the remote server, and the remote server responds with a 404 status code.
* **HTTP Code**: `200 OK` (payload success is `true` because the audit successfully completed). The parsed payload contains `"httpStatus": 404` and the corresponding page title.
* **Logs**: Normal `audit_request_completed` execution log.
* **Recovery**: Ensure target resource actually exists at the requested path.

### 2.4 Remote Server Times Out
* **System Action**: The target server does not respond within the configured timeout period (default: `10s`). WebClient triggers a ReadTimeout / Timeout exception.
* **HTTP Code**: `504 Gateway Timeout` (payload: `{"success":false,"error":{"code":"TIMEOUT","message":"..."}}`).
* **Logs**: `AuditServiceImpl` catches the exception and logs it as a warning: `WARN  [requestId] com.digitalheroes.urlaudit.service.AuditServiceImpl - request_timeout`
* **Recovery**: Resubmit the audit once target server load decreases, or adjust `URL_AUDIT_REQUEST_TIMEOUT` properties.

### 2.5 Rate Limit Exceeded
* **System Action**: Client IP exceeds the limit of 100 requests per minute. The servlet filter intercepts the request and blocks it before it hits the Controller.
* **HTTP Code**: `429 Too Many Requests` (payload: `{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"..."}}`).
* **Logs**: `RateLimitingFilter` logs: `WARN  [requestId] com.digitalheroes.urlaudit.config.RateLimitingFilter - rate_limit_breach for IP: ...`
* **Recovery**: Wait for the rate limit bucket to refresh.

### 2.6 Maximum Concurrent Audits Reached
* **System Action**: Active concurrent audit executions exceed the max limit (default: 10). The backend's semaphore rejects new incoming requests immediately.
* **HTTP Code**: `503 Service Unavailable` (payload: `{"success":false,"error":{"code":"CONCURRENCY_LIMIT_EXCEEDED","message":"..."}}`).
* **Logs**: `AuditServiceImpl` logs a warning when the semaphore cannot be acquired.
* **Recovery**: Resubmit the request in a few seconds once active connections scale down.

### 2.7 Backend Unavailable
* **System Action**: The backend service on Render is offline, crashing, or rebuilding, preventing browser connections.
* **HTTP Code**: Brokered browser error (`net::ERR_CONNECTION_REFUSED` or similar).
* **Logs**: No backend logs are generated.
* **Recovery**: Operator must check Render dashboard status, logs, or Actuator readiness/liveness state.

### 2.8 Cache Miss vs Cache Hit
* **System Action**:
  * **Cache Miss**: Search in Caffeine yields no value. Outbound WebClient request is initiated, and the result is stored in the cache.
  * **Cache Hit**: Caffeine returns the cached audit model. Outbound request is skipped entirely.
* **HTTP Code**: `200 OK` for both.
* **Logs**: Custom logs indicate:
  * Cache Miss: `INFO [requestId] com.digitalheroes.urlaudit.config.LoggingCaffeineCache - cache_miss: URL: ...`
  * Cache Hit: `INFO [requestId] com.digitalheroes.urlaudit.config.LoggingCaffeineCache - cache_hit: URL: ...`
* **Recovery**: Standard automated cache expiration manages cache lifecycles.

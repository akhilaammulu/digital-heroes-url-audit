# API Contract

This document specifies the REST API contract for the URL Audit Service, covering request schemas, success/error payloads, validation rules, and HTTP response codes.

---

## 1. Request Envelope Structure

All API responses are wrapped in a standard JSON envelope to ensure predictable parsing for clients:

```json
{
  "success": "boolean",
  "data": "object | null",
  "error": {
    "code": "string",
    "message": "string"
  } | null,
  "timestamp": "string (ISO-8601)",
  "requestId": "string (UUID)"
}
```

---

## 2. Audit URL Endpoint

Audits a target website to retrieve its status, response latency, and page title.

* **URL**: `/api/v1/audit`
* **HTTP Method**: `POST`
* **Content-Type**: `application/json`

### Request Payload Schema
```json
{
  "url": "string"
}
```

### Payload Validation Rules
1. **`url`**:
   * Must not be empty or null.
   * Must be a valid HTTP or HTTPS URL starting with `http://` or `https://`.
   * Backend check: Evaluated via standard JSR-380 validation (`@NotBlank` and custom regex `@Pattern`).
   * Frontend check: Evaluated client-side via Zod schema checks.

---

## 3. Response Scenarios

### Scenario A: 200 OK (Audit Success)
Returned when the backend successfully resolves and connects to the target website, regardless of the target website's own status (e.g., target 404 is still a successful audit execution).

```json
{
  "success": true,
  "data": {
    "url": "https://example.com",
    "httpStatus": 200,
    "responseTimeMs": 124,
    "pageTitle": "Example Domain",
    "timestamp": "2026-07-25T07:44:52.538429783Z"
  },
  "error": null,
  "timestamp": "2026-07-25T07:48:18.043843303Z",
  "requestId": "ed958fc8-f062-438d-928a-d72ec01b91ed"
}
```

### Scenario B: 400 Bad Request (Payload Validation Failed)
Returned when the submitted request JSON contains a malformed or empty URL.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "url: Must be a valid URL starting with http:// or https://"
  },
  "timestamp": "2026-07-25T07:48:22.115Z",
  "requestId": "3a0cc31b-4f9e-4c74-90ff-4e78fdecd567"
}
```

### Scenario C: 429 Too Many Requests (Rate Limit Breached)
Returned when an IP client sends more requests than allowed (100 requests per minute capacity).

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later."
  },
  "timestamp": "2026-07-25T07:48:25.567Z",
  "requestId": "8f8b8a9c-0cdd-4dd3-82ef-66f81e813a0e"
}
```

### Scenario D: 502 Bad Gateway (Target Resolve/DNS Failure)
Returned when the target website's host cannot be resolved or is unreachable.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DNS_FAILURE",
    "message": "DNS resolution failed for the URL"
  },
  "timestamp": "2026-07-25T07:48:30.124Z",
  "requestId": "a001b9ee-34aa-43e9-9182-1ddc8b320ffb"
}
```

### Scenario E: 503 Service Unavailable (System Overload)
Returned when the server concurrency limit is exceeded (maximum of 10 concurrent active audits).

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "CONCURRENCY_LIMIT_EXCEEDED",
    "message": "Service is busy. Too many concurrent audits."
  },
  "timestamp": "2026-07-25T07:48:35.889Z",
  "requestId": "f89837c7-de89-4fa2-939e-4b44917a224a"
}
```

### Scenario F: 504 Gateway Timeout (Target Slow/Unresponsive)
Returned when the target website takes longer than 10 seconds to respond.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TIMEOUT",
    "message": "The remote server request timed out"
  },
  "timestamp": "2026-07-25T07:48:40.445Z",
  "requestId": "de30f9a2-99ab-41c9-8d89-9a2cdd33bb32"
}
```

---

## 4. Actuator Health Endpoints

Used by deployment services (like Render) to track server health and schedule restarts.

### 4.1 Overall Health
* **URL**: `/actuator/health`
* **Method**: `GET`
* **Response**:
```json
{
  "status": "UP",
  "groups": [
    "liveness",
    "readiness"
  ]
}
```

### 4.2 Liveness Probe
* **URL**: `/actuator/health/liveness`
* **Method**: `GET`
* **Response**:
```json
{
  "status": "UP"
}
```

### 4.3 Readiness Probe
* **URL**: `/actuator/health/readiness`
* **Method**: `GET`
* **Response**:
```json
{
  "status": "UP"
}
```

## 5. API Documentation & Swagger UI

The backend utilizes **Springdoc OpenAPI** to dynamically generate OpenAPI v3 specifications and serve an interactive Swagger UI playground directly from the running application instance.

### 5.1 Configuration Dependency
Swagger support is enabled via the following Maven dependency in `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.17</version>
</dependency>
```

### 5.2 Local Development URLs
* **Swagger UI (Interactive Playground)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Raw OpenAPI JSON Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 5.3 Live Production URLs (Render Deployment)
* **Swagger UI (Interactive Playground)**: [https://url-audit-backend.onrender.com/swagger-ui.html](https://url-audit-backend.onrender.com/swagger-ui.html)
* **Raw OpenAPI JSON Specification**: [https://url-audit-backend.onrender.com/v3/api-docs](https://url-audit-backend.onrender.com/v3/api-docs)


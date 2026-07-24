# Digital Heroes URL Audit

Production-oriented full-stack application for auditing URLs. This repository is intentionally being built milestone by milestone for the SDE assignment.

## Milestone 3: Core URL Audit API

The repository layout and technology choices are established. The backend now contains the Spring Boot foundation, Actuator health endpoint, request correlation, reusable API responses, centralized validation error handling, and the first URL audit API.

## Repository layout

```text
digital-heroes-url-audit/
├── backend/                         # Spring Boot API
├── frontend/                        # Next.js web application
├── docs/                            # Architecture and setup decisions
├── .github/                         # CI/CD and contribution configuration
├── .gitignore
└── README.md
```

### Target application layout

The generated application code will follow this structure:

```text
backend/
├── src/main/java/com/digitalheroes/urlaudit/
│   ├── adapter/
│   │   ├── in/web/                  # HTTP controllers and request/response DTOs
│   │   └── out/                     # External-system adapters
│   ├── application/
│   │   ├── port/in/                 # Use-case interfaces
│   │   ├── port/out/                # Outbound dependency interfaces
│   │   └── service/                 # Use-case implementations
│   ├── config/                      # Framework and application configuration
│   └── domain/                      # Business concepts and rules
├── src/main/resources/
└── src/test/java/com/digitalheroes/urlaudit/

frontend/
├── public/
└── src/
    ├── app/                         # Next.js App Router routes and layouts
    ├── components/                  # Reusable UI components
    ├── lib/                         # API clients and framework-independent helpers
    └── types/                       # Shared TypeScript types
```

## Architecture decisions

- The backend uses clean architecture boundaries: domain code is independent of Spring, application services depend on ports, and web/infrastructure code stays at the adapters.
- The `com.digitalheroes.urlaudit` package root keeps framework bootstrapping and future business modules cohesive without prematurely choosing a domain-specific module name.
- Validation belongs at the web boundary for malformed requests; business invariants will remain in the domain/application layers when Milestone 2 begins.
- The frontend uses the Next.js App Router with a `src` directory so route files remain separate from reusable UI, API, and type modules.
- Maven and npm remain managed independently inside one repository. Nested Git repositories are disabled so the root repository owns the complete application.
- Persistence, authentication, deployment, and URL-audit behavior are deliberately deferred until their respective milestones.

## Initial setup

1. The backend foundation follows [`docs/spring-initializr-config.json`](docs/spring-initializr-config.json), using Spring Boot `3.5.4` and Java `21`.
2. From the repository root, create the frontend when frontend work begins:

   ```bash
   npx create-next-app@15 frontend \
     --typescript \
     --tailwind \
     --eslint \
     --app \
     --src-dir \
     --use-npm \
     --import-alias "@/*" \
     --disable-git
   ```

3. Confirm that the generated frontend builds independently before starting the next milestone.

## Audit API

`POST /api/v1/audit` accepts a valid HTTP or HTTPS URL and returns the fetched URL, HTTP status, response time in milliseconds, page title, and audit timestamp inside the standard API response envelope. The first version intentionally has no caching, retries, rate limiting, concurrency control, or audit logging.

OpenAPI documentation is provided through Springdoc OpenAPI and Swagger UI:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17`

```json
{
  "url": "https://example.com"
}
```

## Production hardening

- Logging uses SLF4J structured key-value events. `RequestIdFilter` places the correlation ID in the SLF4J MDC, and the console pattern includes it on every request-scoped application log line. Audit logs include request receipt, target URL, response status, response time, failures, and completion duration without logging response bodies.
- Error mapping keeps client-facing failures stable: `INVALID_URL` returns `400`, `TIMEOUT` returns `504`, `DNS_FAILURE` and `CONNECTION_FAILURE` return `502`, and unexpected failures return `500` with `UNEXPECTED_ERROR`.
- Correlation IDs let a client trace one request across the API response, application logs, and downstream URL-fetch activity without exposing internal exception details in the response.
- Actuator readiness and liveness probes are available at `/actuator/health/readiness` and `/actuator/health/liveness`.

Caching, rate limiting, retries, circuit breakers, and queues remain intentionally deferred.

## Development commands

Backend commands will be run from `backend/`:

```bash
mvn spring-boot:run
mvn test
```

Frontend commands will be run from `frontend/`:

```bash
npm run dev
npm run lint
npm run build
```

## Git convention

The first commit should be:

```text
chore: initialize full-stack project structure
```

The next milestone starts only after this backend foundation has been reviewed and approved.

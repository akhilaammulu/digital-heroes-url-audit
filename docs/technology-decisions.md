# Technology Decisions

This document outlines the architectural and engineering rationale behind the selection of the core technologies, frameworks, and deployment platforms used in the URL Audit Service.

---

## 1. Programming Languages & Core Runtimes

### Java 21
* **Why Chosen**: It is the latest industry-standard Long-Term Support (LTS) release of the Java language, offering modern features while preserving JVM stability and compatibility.
* **Advantages**:
  * Native **Records** (used for DTOs and properties binding like `UrlAuditProperties.java`) eliminate standard Lombok/POJO boilerplate.
  * Pattern Matching and Switch expressions improve logic readability.
  * Enhanced runtime performance and optimized garbage collection.
* **Alternatives Considered**: Java 17, Kotlin.
* **Trade-offs**: Slightly larger RAM footprint and longer startup times compared to languages like Go or Rust, but has a superior library ecosystem for enterprise integrations.

### TypeScript 5.x
* **Why Chosen**: Provides strict static typing on top of JavaScript, improving client-side safety and overall IDE productivity.
* **Advantages**:
  * Prevents runtime type conversion crashes.
  * Strongly defines shared API request/response structures between backend and frontend.
  * Rich autocomplete and self-documenting code.
* **Alternatives Considered**: Vanilla JavaScript (ES6+).
* **Trade-offs**: Requires compilation build steps, which slightly increases frontend build time, but avoids class-wide runtime regressions.

---

## 2. Backend Frameworks & Libraries

### Spring Boot 3.5.x
* **Why Chosen**: The dominant enterprise Java framework, selected to leverage automated configuration, rapid REST API bootstrapping, and mature ecosystem integrations.
* **Advantages**:
  * Out-of-the-box **Spring Boot Actuator** (provides robust `/actuator/health/readiness` and `liveness` checks).
  * Auto-configured servlet filter execution ordering.
  * Simplified testing using `@SpringBootTest` and WebMvc test slice mocks.
* **Alternatives Considered**: Quarkus, Micronaut.
* **Trade-offs**: Slower initial cold-start times (especially noticeable on Render's free tier) compared to lightweight cloud-native runtimes, but has unrivaled developer support.

### Spring WebClient (Reactive HTTP Client)
* **Why Chosen**: Built-in reactive HTTP client from the Spring WebFlux library, used to execute non-blocking outbound requests during audits.
* **Advantages**:
  * Multi-thread concurrency: does not tie up worker threads while waiting for remote websites to respond.
  * Built-in timeout controls and support for large response buffers (up to 16 MB configured in `WebClientConfig`).
  * Automatic redirect following capabilities.
* **Alternatives Considered**: RestTemplate (blocking), OpenFeign.
* **Trade-offs**: Introduces reactive programming concepts (`Mono`/`Flux`) which require careful handling of subscription contexts, but is much more resource-efficient than blocking clients under high load.

### Caffeine Cache
* **Why Chosen**: A high-performance, in-memory Java caching library offering near-optimal expiration policies.
* **Advantages**:
  * Bounded cache limits (`maximumSize`) prevent JVM OutOfMemory errors.
  * Efficient concurrency control using window-tinyLFU eviction policies.
  * Integrates seamlessly with Spring's `@Cacheable` and custom wrappers.
* **Alternatives Considered**: Redis, Ehcache.
* **Trade-offs**: Cache is local to the JVM instance (not distributed). If the backend scales horizontally, caches are not synchronized. However, for a single-instance container, it avoids the cost and complexity of spinning up a separate database.

### Bucket4j
* **Why Chosen**: A robust, thread-safe Java rate limiting library based on the token-bucket algorithm.
* **Advantages**:
  * Extremely fast execution times with minimal lock overhead.
  * Integrates with Caffeine as an in-memory storage manager for IP-rate-limit state.
  * Precise configuration of capacities and refill intervals.
* **Alternatives Considered**: Spring Cloud Gateway rate limiter, Redis-based rate limiting.
* **Trade-offs**: Like Caffeine, it is local to the single server instance. However, this satisfies local single-node protection without adding infrastructure dependencies.

---

## 3. Frontend Frameworks & Client-Side Tools

### Next.js 15 (App Router)
* **Why Chosen**: Modern React framework with native support for server-side rendering, routing, static optimization, and modular UI structure.
* **Advantages**:
  * Fast compilation and optimization via Next.js Turbopack.
  * Modular styling using Tailwind CSS out of the box.
  * Structured page/layout segregation.
* **Alternatives Considered**: Create React App (Vite SPA), Remix.
* **Trade-offs**: Adds framework overhead and specific directory constraints, but provides excellent, modern UX and built-in production optimizations.

### React Hook Form
* **Why Chosen**: Performance-oriented React library for managing form inputs without unnecessary re-renders.
* **Advantages**:
  * Uses uncontrolled components to maximize input performance.
  * Integrates perfectly with Zod schemas for schema-based validation.
  * Minimal boilerplate code for handling submission states.
* **Alternatives Considered**: Formik, raw React state.
* **Trade-offs**: Standard learning curve, but saves substantial rendering overhead when validating input streams.

### Zod
* **Why Chosen**: TypeScript-first schema declaration and validation library.
* **Advantages**:
  * Synchronizes TypeScript interface typing with runtime payload schema validation.
  * Automatically isolates invalid URL formats client-side before sending requests.
  * Simplifies custom error messages.
* **Alternatives Considered**: Yup, Joi.
* **Trade-offs**: Adds extra size to the production client JS bundle, but provides robust runtime type protection.

### Axios
* **Why Chosen**: Promised-based HTTP client for client browsers.
* **Advantages**:
  * Automatic JSON data transformation.
  * Interceptor support and cleaner error handling compared to the browser's native `fetch` API.
  * Configures timeouts and request abort sequences easily.
* **Alternatives Considered**: Browser Native Fetch API.
* **Trade-offs**: Slightly increases bundle footprint, but Axios is more predictable and handles HTTP status errors (like 429/503/504) natively in catch blocks.

---

## 4. DevOps, Automation, & Deployment

### GitHub Actions
* **Why Chosen**: Automation server integrated directly with the GitHub repository.
* **Advantages**:
  * Automated CI pipeline checks on pull requests and pushes to `main`.
  * Pre-configured JDK environments and caching mechanisms for Maven dependencies.
  * Clear log tracking and badge status updates.
* **Alternatives Considered**: Jenkins, CircleCI.
* **Trade-offs**: Dependent on GitHub infrastructure availability, but requires zero maintenance.

### Render
* **Why Chosen**: Unified cloud hosting platform that simplifies full-stack web application hosting.
* **Advantages**:
  * Blueprint support (`render.yaml`): allows one-click stack setup.
  * Native Docker container support (excellent for Java Alpine runtimes).
  * Automatically provisions SSL/TLS certificates and sets up secure public routing.
* **Alternatives Considered**: AWS ECS, Heroku, Railway.
* **Trade-offs**: The Free tier automatically suspends inactive web services, which introduces a 30-50s cold-start latency when waking up. However, it is entirely free and sufficient for demo/verification environments.

### Vercel
* **Why Chosen**: Serverless hosting platform optimized specifically for Next.js applications.
* **Advantages**:
  * Near-instant static content loading via Vercel Edge Network.
  * Zero-configuration deployment for Next.js Apps.
  * Fast deployments.
* **Alternatives Considered**: Render static site hosting, AWS Amplify.
* **Trade-offs**: While Vercel is the ultimate target for Next.js, our current configuration maps the frontend as a Node service on Render to keep the entire system consolidated under a single Render Blueprint for simplified administration.

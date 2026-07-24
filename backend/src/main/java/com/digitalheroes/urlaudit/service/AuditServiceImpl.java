package com.digitalheroes.urlaudit.service;

import com.digitalheroes.urlaudit.config.UrlAuditProperties;
import com.digitalheroes.urlaudit.dto.AuditRequest;
import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.exception.AuditErrorCode;
import com.digitalheroes.urlaudit.exception.UrlAuditException;
import com.digitalheroes.urlaudit.util.RequestIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.HtmlUtils;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?is)<title\\b[^>]*>(.*?)</title>");

    private final WebClient webClient;
    private final UrlAuditProperties properties;
    private final Semaphore semaphore;

    public AuditServiceImpl(WebClient auditWebClient, UrlAuditProperties properties) {
        this.webClient = auditWebClient;
        this.properties = properties;
        this.semaphore = new Semaphore(properties.concurrency().maxConcurrentAudits());
    }

    @Override
    @Cacheable(
            cacheNames = "auditResponses",
            key = "#request.url()",
            unless = "#result == null || #result.httpStatus() < 200 || #result.httpStatus() >= 300")
    public AuditResponse audit(AuditRequest request) {
        String requestId = RequestIdUtils.current();
        long startNanos = System.nanoTime();

        if (!semaphore.tryAcquire()) {
            log.atWarn()
                    .addKeyValue("event", "concurrency_limit_exceeded")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("url", request.url())
                    .log("concurrency_limit_exceeded");
            throw new UrlAuditException(
                    AuditErrorCode.CONCURRENCY_LIMIT_EXCEEDED,
                    "Too many concurrent audits. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        try {
            log.atInfo()
                    .addKeyValue("event", "audit_started")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("url", request.url())
                    .log("audit_started");

            URI targetUri = toHttpUri(request.url());
            ResponseEntity<String> response = fetch(targetUri);
            long responseTimeMs = elapsedMs(startNanos);
            String pageTitle = extractPageTitle(response.getBody());

            log.atInfo()
                    .addKeyValue("event", "audit_response_received")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("url", request.url())
                    .addKeyValue("status", response.getStatusCode().value())
                    .addKeyValue("responseTimeMs", responseTimeMs)
                    .log("audit_response_received");

            return new AuditResponse(
                    request.url(),
                    response.getStatusCode().value(),
                    responseTimeMs,
                    pageTitle,
                    Instant.now());
        } catch (UrlAuditException exception) {
            logFailure(request.url(), requestId, exception, startNanos);
            throw exception;
        } catch (WebClientException exception) {
            UrlAuditException mappedException = mapWebClientException(exception);
            logFailure(request.url(), requestId, mappedException, startNanos);
            throw mappedException;
        } catch (RuntimeException exception) {
            UrlAuditException mappedException = containsTimeout(exception)
                    ? timeoutException(exception)
                    : new UrlAuditException(
                    AuditErrorCode.UNEXPECTED_ERROR,
                    "An unexpected error occurred while auditing the URL",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
            logFailure(request.url(), requestId, mappedException, startNanos);
            throw mappedException;
        } finally {
            semaphore.release();
        }
    }

    private ResponseEntity<String> fetch(URI targetUri) {
        ResponseEntity<String> response = webClient.get()
                .uri(targetUri)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                .timeout(properties.requestTimeout())
                .block();
        if (response == null) {
            throw new UrlAuditException(
                    AuditErrorCode.UNEXPECTED_ERROR,
                    "The URL returned no response",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private UrlAuditException mapWebClientException(WebClientException exception) {
        if (containsCause(exception, UnknownHostException.class)) {
            return new UrlAuditException(
                    AuditErrorCode.DNS_FAILURE,
                    "DNS resolution failed for the URL",
                    HttpStatus.BAD_GATEWAY,
                    exception);
        }
        if (containsTimeout(exception)) {
            return timeoutException(exception);
        }
        if (containsCause(exception, ConnectException.class)
                || containsCause(exception, NoRouteToHostException.class)
                || containsCause(exception, SocketTimeoutException.class)) {
            return new UrlAuditException(
                    AuditErrorCode.CONNECTION_FAILURE,
                    "The URL connection could not be established",
                    HttpStatus.BAD_GATEWAY,
                    exception);
        }
        return new UrlAuditException(
                AuditErrorCode.CONNECTION_FAILURE,
                "The URL connection could not be established",
                HttpStatus.BAD_GATEWAY,
                exception);
    }

    private UrlAuditException timeoutException(Throwable cause) {
        return new UrlAuditException(
                AuditErrorCode.TIMEOUT,
                "The URL request timed out",
                HttpStatus.GATEWAY_TIMEOUT,
                cause);
    }

    private URI toHttpUri(String value) {
        if (value == null || value.isBlank()) {
            throw invalidUrl();
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw invalidUrl();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw invalidUrl();
        }
    }

    private UrlAuditException invalidUrl() {
        return new UrlAuditException(
                AuditErrorCode.INVALID_URL,
                "url must be a valid HTTP or HTTPS URL",
                HttpStatus.BAD_REQUEST);
    }

    private boolean containsTimeout(Throwable exception) {
        return containsCause(exception, TimeoutException.class)
                || containsCause(exception, SocketTimeoutException.class);
    }

    private boolean containsCause(Throwable exception, Class<? extends Throwable> targetType) {
        Throwable current = exception;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void logFailure(
            String url,
            String requestId,
            UrlAuditException exception,
            long startNanos) {
        log.atError()
                .addKeyValue("event", "audit_failed")
                .addKeyValue("requestId", requestId)
                .addKeyValue("url", url)
                .addKeyValue("errorCode", exception.getCode())
                .addKeyValue("durationMs", elapsedMs(startNanos))
                .setCause(exception)
                .log("audit_failed");
    }

    private String extractPageTitle(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        Matcher matcher = TITLE_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }

        String title = HtmlUtils.htmlUnescape(matcher.group(1))
                .replaceAll("\\s+", " ")
                .trim();
        return title.isEmpty() ? null : title;
    }
}

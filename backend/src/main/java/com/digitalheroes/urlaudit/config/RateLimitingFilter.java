package com.digitalheroes.urlaudit.config;

import com.digitalheroes.urlaudit.exception.AuditErrorCode;
import com.digitalheroes.urlaudit.exception.UrlAuditException;
import com.digitalheroes.urlaudit.util.RequestIdUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final UrlAuditProperties properties;
    private final HandlerExceptionResolver resolver;
    private final Cache<String, Bucket> ipBuckets;

    public RateLimitingFilter(
            UrlAuditProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
        this.ipBuckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("/api/v1/audit".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            String ip = getClientIp(request);
            Bucket bucket = ipBuckets.get(ip, key -> createNewBucket());

            if (!bucket.tryConsume(1)) {
                String requestId = RequestIdUtils.from(request);
                log.atWarn()
                        .addKeyValue("event", "rate_limit_exceeded")
                        .addKeyValue("requestId", requestId)
                        .addKeyValue("ip", ip)
                        .log("rate_limit_exceeded");

                UrlAuditException exception = new UrlAuditException(
                        AuditErrorCode.RATE_LIMIT_EXCEEDED,
                        "Rate limit exceeded. Maximum 100 requests per minute per IP.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
                resolver.resolveException(request, response, null, exception);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createNewBucket() {
        var rateLimit = properties.rateLimit();
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimit.capacity())
                .refillIntervally(rateLimit.refillTokens(), rateLimit.duration())
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

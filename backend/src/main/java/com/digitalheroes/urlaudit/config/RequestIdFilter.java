package com.digitalheroes.urlaudit.config;

import com.digitalheroes.urlaudit.util.RequestIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestIdUtils.resolve(request.getHeader(RequestIdUtils.HEADER_NAME));
        request.setAttribute(RequestIdUtils.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(RequestIdUtils.HEADER_NAME, requestId);
        String previousRequestId = MDC.get(RequestIdUtils.MDC_KEY);
        MDC.put(RequestIdUtils.MDC_KEY, requestId);

        boolean auditRequest = "/api/v1/audit".equals(request.getRequestURI());
        long startNanos = System.nanoTime();
        if (auditRequest) {
            log.atInfo()
                    .addKeyValue("event", "request_received")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .log("audit_request_received");
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (auditRequest) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                if (response.getStatus() >= 400) {
                    log.atWarn()
                            .addKeyValue("event", "request_failed")
                            .addKeyValue("requestId", requestId)
                            .addKeyValue("status", response.getStatus())
                            .log("audit_request_failed");
                }
                log.atInfo()
                        .addKeyValue("event", "request_completed")
                        .addKeyValue("requestId", requestId)
                        .addKeyValue("status", response.getStatus())
                        .addKeyValue("durationMs", durationMs)
                        .log("audit_request_completed");
            }
            if (previousRequestId == null) {
                MDC.remove(RequestIdUtils.MDC_KEY);
            } else {
                MDC.put(RequestIdUtils.MDC_KEY, previousRequestId);
            }
        }
    }
}

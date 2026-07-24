package com.digitalheroes.urlaudit.config;

import com.digitalheroes.urlaudit.exception.UrlAuditException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        UrlAuditProperties properties = new UrlAuditProperties(
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
                1000,
                new UrlAuditProperties.RateLimit(2, 2, Duration.ofSeconds(1)),
                new UrlAuditProperties.Concurrency(10)
        );
        filter = new RateLimitingFilter(properties, resolver);
    }

    @Test
    void allowsRequestsUnderLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/audit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    void blocksRequestsOverLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/audit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(resolver, times(1)).resolveException(eq(request), eq(response), any(), any(UrlAuditException.class));
    }

    @Test
    void usesXForwardedForHeaderForIpResolution() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/audit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    void skipsFilterForNonAuditUrls() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(resolver);
    }
}

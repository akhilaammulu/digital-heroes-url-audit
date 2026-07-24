package com.digitalheroes.urlaudit.service;

import com.digitalheroes.urlaudit.config.CacheConfig;
import com.digitalheroes.urlaudit.dto.AuditRequest;
import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.exception.UrlAuditException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(AuditServiceCachingIntegrationTest.TestWebClientConfiguration.class)
@TestPropertySource(properties = {
        "url-audit.cache-ttl=100ms",
        "url-audit.cache-maximum-size=10"
})
class AuditServiceCachingIntegrationTest {

    private static final String AUDIT_URL = "https://example.com";

    @org.springframework.beans.factory.annotation.Autowired
    private AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    private CacheManager cacheManager;

    @org.springframework.beans.factory.annotation.Autowired
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.AUDIT_CACHE_NAME).clear();
        reset(exchangeFunction);
    }

    @Test
    void cacheMissFetchesAndStoresSuccessfulResponse() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response(HttpStatus.OK, "<title>Example</title>")));

        AuditResponse result = auditService.audit(new AuditRequest(AUDIT_URL));

        assertThat(result.httpStatus()).isEqualTo(200);
        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }

    @Test
    void cacheHitAvoidsSecondExternalRequest() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response(HttpStatus.OK, "<title>Example</title>")));

        AuditResponse firstResult = auditService.audit(new AuditRequest(AUDIT_URL));
        AuditResponse secondResult = auditService.audit(new AuditRequest(AUDIT_URL));

        assertThat(secondResult).isEqualTo(firstResult);
        verify(exchangeFunction, times(1)).exchange(any(ClientRequest.class));
    }

    @Test
    void cacheEntryExpiresAfterConfiguredTtl() throws InterruptedException {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenAnswer(invocation -> Mono.just(response(HttpStatus.OK, "<title>Example</title>")));

        auditService.audit(new AuditRequest(AUDIT_URL));
        Thread.sleep(150);
        auditService.audit(new AuditRequest(AUDIT_URL));

        verify(exchangeFunction, times(2)).exchange(any(ClientRequest.class));
    }

    @Test
    void failedResponseIsNotCached() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(
                        Mono.just(response(HttpStatus.BAD_GATEWAY, "<title>Failure</title>")),
                        Mono.just(response(HttpStatus.OK, "<title>Recovered</title>")));

        AuditResponse failedResult = auditService.audit(new AuditRequest(AUDIT_URL));
        AuditResponse recoveredResult = auditService.audit(new AuditRequest(AUDIT_URL));

        assertThat(failedResult.httpStatus()).isEqualTo(502);
        assertThat(recoveredResult.httpStatus()).isEqualTo(200);
        verify(exchangeFunction, times(2)).exchange(any(ClientRequest.class));
    }

    @Test
    void exceptionIsNotCached() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(
                        Mono.error(requestException()),
                        Mono.just(response(HttpStatus.OK, "<title>Recovered</title>")));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> auditService.audit(new AuditRequest(AUDIT_URL)));
        AuditResponse recoveredResult = auditService.audit(new AuditRequest(AUDIT_URL));

        assertThat(thrown).isInstanceOf(UrlAuditException.class);
        assertThat(recoveredResult.httpStatus()).isEqualTo(200);
        verify(exchangeFunction, times(2)).exchange(any(ClientRequest.class));
    }

    @Test
    void invalidUrlIsNotCached() {
        String invalidUrl = "ftp://example.com";

        Throwable firstThrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> auditService.audit(new AuditRequest(invalidUrl)));
        Throwable secondThrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> auditService.audit(new AuditRequest(invalidUrl)));

        assertThat(firstThrown).isInstanceOf(UrlAuditException.class);
        assertThat(secondThrown).isInstanceOf(UrlAuditException.class);
        assertThat(cacheManager.getCache(CacheConfig.AUDIT_CACHE_NAME).get(invalidUrl)).isNull();
    }

    private ClientResponse response(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(body)
                .build();
    }

    private org.springframework.web.reactive.function.client.WebClientRequestException requestException() {
        return new org.springframework.web.reactive.function.client.WebClientRequestException(
                new ConnectException("connection refused"),
                HttpMethod.GET,
                URI.create(AUDIT_URL),
                HttpHeaders.EMPTY);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestWebClientConfiguration {

        @Bean
        ExchangeFunction exchangeFunction() {
            return Mockito.mock(ExchangeFunction.class);
        }

        @Bean
        @Primary
        WebClient testWebClient(ExchangeFunction exchangeFunction) {
            return WebClient.builder()
                    .exchangeFunction(exchangeFunction)
                    .build();
        }
    }
}

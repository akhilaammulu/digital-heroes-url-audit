package com.digitalheroes.urlaudit.service;

import com.digitalheroes.urlaudit.config.UrlAuditProperties;
import com.digitalheroes.urlaudit.dto.AuditRequest;
import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.exception.UrlAuditException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        UrlAuditProperties properties = new UrlAuditProperties(
                Duration.ofMillis(100),
                Duration.ofMinutes(5),
                100);
        auditService = new AuditServiceImpl(webClient, properties);
    }

    @Test
    void returnsHttpStatusResponseTimeAndPageTitle() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response(HttpStatus.OK, "<html><title> Example Domain </title></html>")));

        AuditResponse result = auditService.audit(new AuditRequest("https://example.com"));

        assertThat(result.url()).isEqualTo("https://example.com");
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.responseTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.pageTitle()).isEqualTo("Example Domain");
        assertThat(result.timestamp()).isNotNull();

        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(requestCaptor.capture());
        assertThat(requestCaptor.getValue().method()).isEqualTo(HttpMethod.GET);
        assertThat(requestCaptor.getValue().url()).hasToString("https://example.com");
    }

    @Test
    void preservesNonSuccessHttpStatusAsAuditResult() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response(HttpStatus.NOT_FOUND, "<title>Missing</title>")));

        AuditResponse result = auditService.audit(new AuditRequest("https://example.com/missing"));

        assertThat(result.httpStatus()).isEqualTo(404);
        assertThat(result.pageTitle()).isEqualTo("Missing");
    }

    @Test
    void convertsTimeoutIntoAuditException() {
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.never());

        Throwable thrown = catchThrowable(() -> auditService.audit(new AuditRequest("https://example.com")));

        assertAuditFailure(thrown, "TIMEOUT", HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void rejectsInvalidUrl() {
        Throwable thrown = catchThrowable(() -> auditService.audit(new AuditRequest("ftp://example.com")));

        assertAuditFailure(thrown, "INVALID_URL", HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsDnsFailure() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.error(requestException(new UnknownHostException("example.com"))));

        Throwable thrown = catchThrowable(() -> auditService.audit(new AuditRequest("https://example.com")));

        assertAuditFailure(thrown, "DNS_FAILURE", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void mapsConnectionFailure() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.error(requestException(new ConnectException("connection refused"))));

        Throwable thrown = catchThrowable(() -> auditService.audit(new AuditRequest("https://example.com")));

        assertAuditFailure(thrown, "CONNECTION_FAILURE", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void rejectsRequestsExceedingConcurrencyLimit() throws Exception {
        UrlAuditProperties properties = new UrlAuditProperties(
                Duration.ofMillis(100),
                Duration.ofMinutes(5),
                100,
                new UrlAuditProperties.RateLimit(100, 100, Duration.ofMinutes(1)),
                new UrlAuditProperties.Concurrency(1)
        );
        AuditServiceImpl singleAuditService = new AuditServiceImpl(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.never());

        Thread thread = new Thread(() -> {
            try {
                singleAuditService.audit(new AuditRequest("https://example.com"));
            } catch (Exception ignored) {}
        });
        thread.start();

        Thread.sleep(50);

        Throwable thrown = catchThrowable(() -> singleAuditService.audit(new AuditRequest("https://example.com")));

        assertAuditFailure(thrown, "CONCURRENCY_LIMIT_EXCEEDED", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private ClientResponse response(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .body(body)
                .build();
    }

    private WebClientRequestException requestException(Throwable cause) {
        return new WebClientRequestException(
                cause,
                HttpMethod.GET,
                URI.create("https://example.com"),
                HttpHeaders.EMPTY);
    }

    private void assertAuditFailure(Throwable thrown, String code, HttpStatus status) {
        assertThat(thrown).isInstanceOf(UrlAuditException.class);
        UrlAuditException exception = (UrlAuditException) thrown;
        assertThat(exception.getCode()).isEqualTo(code);
        assertThat(exception.getStatus()).isEqualTo(status);
    }
}

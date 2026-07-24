package com.digitalheroes.urlaudit.controller;

import com.digitalheroes.urlaudit.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuditControllerConcurrencyIntegrationTest.TestWebClientConfiguration.class)
@TestPropertySource(properties = {
        "url-audit.concurrency.max-concurrent-audits=1",
        "url-audit.rate-limit.capacity=100",
        "url-audit.rate-limit.refill-tokens=100"
})
class AuditControllerConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeFunction exchangeFunction;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.AUDIT_CACHE_NAME).clear();
        Mockito.reset(exchangeFunction);
    }

    @Test
    void rejectsRequestsExceedingConcurrencyLimit() throws Exception {
        CountDownLatch firstRequestStarted = new CountDownLatch(1);
        CountDownLatch resumeFirstRequest = new CountDownLatch(1);

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenAnswer(invocation -> {
                    firstRequestStarted.countDown();
                    resumeFirstRequest.await();
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                            .body("<html><title>Success</title></html>")
                            .build());
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                mockMvc.perform(post("/api/v1/audit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"url\":\"https://example-concurrent-1.com\"}"))
                        .andExpect(status().isOk());
            } catch (Exception ignored) {}
        });

        boolean started = firstRequestStarted.await(5, TimeUnit.SECONDS);
        assertThat(started).isTrue();

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example-concurrent-2.com\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONCURRENCY_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value("Too many concurrent audits. Please try again later."));

        resumeFirstRequest.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
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

package com.digitalheroes.urlaudit.controller;

import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "url-audit.rate-limit.capacity=2",
        "url-audit.rate-limit.refill-tokens=2",
        "url-audit.rate-limit.duration=10s"
})
class AuditControllerRateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void rateLimitsRequestsBeyondCapacity() throws Exception {
        when(auditService.audit(any()))
                .thenReturn(new AuditResponse("https://example.com", 200, 10, "Example", Instant.now()));

        String requestBody = "{\"url\":\"https://example.com\"}";

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value("Rate limit exceeded. Maximum 100 requests per minute per IP."))
                .andExpect(jsonPath("$.requestId").exists());
    }
}

package com.digitalheroes.urlaudit.controller;

import com.digitalheroes.urlaudit.config.RequestIdFilter;
import com.digitalheroes.urlaudit.config.UrlAuditProperties;
import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.exception.GlobalExceptionHandler;
import com.digitalheroes.urlaudit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import({RequestIdFilter.class, GlobalExceptionHandler.class})
class AuditControllerWebMvcTest {

    private static final String REQUEST_ID = "audit-test-request";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private UrlAuditProperties urlAuditProperties;

    @Test
    void includesRequestIdInSuccessfulResponse() throws Exception {
        when(auditService.audit(any()))
                .thenReturn(new AuditResponse(
                        "https://example.com",
                        200,
                        42,
                        "Example Domain",
                        Instant.parse("2026-07-24T00:00:00Z")));

        mockMvc.perform(post("/api/v1/audit")
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.httpStatus").value(200))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
    }

    @Test
    void includesRequestIdAndInvalidUrlCodeInValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/audit")
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_URL"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
    }
}

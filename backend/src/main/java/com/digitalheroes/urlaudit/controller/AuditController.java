package com.digitalheroes.urlaudit.controller;

import com.digitalheroes.urlaudit.dto.ApiResponse;
import com.digitalheroes.urlaudit.dto.AuditRequest;
import com.digitalheroes.urlaudit.dto.AuditResponse;
import com.digitalheroes.urlaudit.service.AuditService;
import com.digitalheroes.urlaudit.util.RequestIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuditResponse>> audit(
            @Valid @RequestBody AuditRequest request,
            HttpServletRequest servletRequest) {
        AuditResponse auditResponse = auditService.audit(request);
        return ResponseEntity.ok(ApiResponse.success(
                auditResponse,
                RequestIdUtils.from(servletRequest)));
    }
}

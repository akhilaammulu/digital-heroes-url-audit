package com.digitalheroes.urlaudit.service;

import com.digitalheroes.urlaudit.dto.AuditRequest;
import com.digitalheroes.urlaudit.dto.AuditResponse;

public interface AuditService {

    AuditResponse audit(AuditRequest request);
}

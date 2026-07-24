package com.digitalheroes.urlaudit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

public record AuditRequest(
        @NotBlank(message = "url is required")
        @URL(message = "url must be a valid URL")
        @Pattern(regexp = "(?i)^https?://.+$", message = "url must use HTTP or HTTPS")
        String url) {
}

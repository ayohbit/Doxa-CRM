package com.doxa.crm.dto.integration;

import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(
        @NotBlank String subject,
        @NotBlank String body
) {
}

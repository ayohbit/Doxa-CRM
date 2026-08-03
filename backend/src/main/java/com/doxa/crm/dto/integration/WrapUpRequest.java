package com.doxa.crm.dto.integration;

import jakarta.validation.constraints.NotBlank;

public record WrapUpRequest(
        @NotBlank String outcome,
        String objection,
        String nextStep
) {
}

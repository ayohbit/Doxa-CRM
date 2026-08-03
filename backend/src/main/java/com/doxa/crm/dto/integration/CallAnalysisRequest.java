package com.doxa.crm.dto.integration;

import jakarta.validation.constraints.NotBlank;

public record CallAnalysisRequest(
        @NotBlank String fathomUrl
) {
}

package com.doxa.crm.dto.developer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record CreateApiTokenRequest(
        @NotBlank @Size(max = 120) String name,
        @NotEmpty Set<String> scopes,
        Instant expiresAt
) {
}

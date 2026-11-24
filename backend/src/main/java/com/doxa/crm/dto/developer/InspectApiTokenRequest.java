package com.doxa.crm.dto.developer;

import jakarta.validation.constraints.NotBlank;

public record InspectApiTokenRequest(@NotBlank String token) {
}

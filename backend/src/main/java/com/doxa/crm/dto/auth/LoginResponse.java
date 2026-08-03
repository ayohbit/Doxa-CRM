package com.doxa.crm.dto.auth;

public record LoginResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}

package com.doxa.crm.dto.auth;

import com.doxa.crm.domain.enums.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID licenseId,
        String email,
        UserRole role,
        String companyName
) {
}

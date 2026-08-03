package com.doxa.crm.dto.user;

import com.doxa.crm.domain.enums.UserRole;

import java.util.UUID;

public record TeamUserResponse(
        UUID id,
        String email,
        UserRole role
) {
}

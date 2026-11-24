package com.doxa.crm.dto.developer;

import java.time.Instant;
import java.util.Set;

public record ApiTokenInspectionResponse(
        String status,
        String owner,
        String name,
        Set<String> scopes,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt
) {
}

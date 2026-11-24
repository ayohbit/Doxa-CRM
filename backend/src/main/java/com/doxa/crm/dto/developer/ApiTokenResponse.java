package com.doxa.crm.dto.developer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ApiTokenResponse(
        UUID id,
        String name,
        String tokenPrefix,
        Set<String> scopes,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt,
        String status
) {
}

package com.doxa.crm.dto.integration;

import java.time.Instant;
import java.util.UUID;

public record TimelineEventResponse(
        UUID id,
        String eventType,
        String title,
        String body,
        Instant createdAt,
        String createdByEmail
) {
}

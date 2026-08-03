package com.doxa.crm.dto.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WrapUpResponse(
        UUID opportunityId,
        String outcome,
        String objection,
        String nextStep,
        Instant updatedAt
) {
}

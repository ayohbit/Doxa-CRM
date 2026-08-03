package com.doxa.crm.dto.opportunity;

import java.math.BigDecimal;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        UUID contactId,
        String name,
        String stageId,
        String adSet,
        String revenueMonthly,
        String createdOn,
        BigDecimal value,
        String email,
        String phone,
        String phoneE164,
        String whatsAppUrl,
        boolean hasWrapUp,
        BigDecimal callScore
) {
}

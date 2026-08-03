package com.doxa.crm.dto.opportunity;

import java.math.BigDecimal;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        String name,
        String stageId,
        String adSet,
        String revenueMonthly,
        String createdOn,
        BigDecimal value,
        String email,
        String phone
) {
}

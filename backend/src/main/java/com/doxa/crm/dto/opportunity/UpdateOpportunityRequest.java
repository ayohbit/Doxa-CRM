package com.doxa.crm.dto.opportunity;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateOpportunityRequest(
        String stageSlug,
        BigDecimal value,
        String adSet,
        String revenueMonthly,
        UUID assignedUserId,
        String status,
        String lostReason
) {
}

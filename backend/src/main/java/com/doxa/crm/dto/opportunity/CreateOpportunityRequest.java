package com.doxa.crm.dto.opportunity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOpportunityRequest(
        @NotNull UUID contactId,
        @NotBlank String stageSlug,
        BigDecimal value,
        String adSet,
        String revenueMonthly,
        UUID assignedUserId
) {
}

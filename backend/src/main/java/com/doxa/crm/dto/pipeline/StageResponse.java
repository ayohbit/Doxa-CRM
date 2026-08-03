package com.doxa.crm.dto.pipeline;

import java.math.BigDecimal;

public record StageResponse(
        String id,
        String name,
        BigDecimal monetaryValue,
        long opportunityCount,
        BigDecimal totalValue
) {
}

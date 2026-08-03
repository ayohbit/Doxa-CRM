package com.doxa.crm.dto.integration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CallAnalysisResponse(
        UUID opportunityId,
        String fathomUrl,
        BigDecimal aiScore,
        String aiSummary,
        Map<String, Integer> dimensionScores
) {
}

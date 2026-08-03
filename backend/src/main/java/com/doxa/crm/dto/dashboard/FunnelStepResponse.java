package com.doxa.crm.dto.dashboard;

public record FunnelStepResponse(
        String stage,
        int pct,
        long count
) {
}

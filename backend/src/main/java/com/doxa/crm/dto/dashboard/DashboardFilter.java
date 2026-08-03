package com.doxa.crm.dto.dashboard;

import java.time.Instant;
import java.util.UUID;

public record DashboardFilter(
        Instant from,
        Instant to,
        UUID assignedUserId,
        String adSet
) {
}

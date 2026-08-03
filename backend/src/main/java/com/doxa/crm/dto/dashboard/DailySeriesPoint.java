package com.doxa.crm.dto.dashboard;

import java.math.BigDecimal;

public record DailySeriesPoint(
        String day,
        BigDecimal spend,
        BigDecimal cashCollected,
        BigDecimal revenue
) {
}

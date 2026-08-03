package com.doxa.crm.dto.dashboard;

import java.math.BigDecimal;

public record DashboardKpisResponse(
        BigDecimal adSpend,
        long leads,
        BigDecimal costPerLead,
        long triage,
        BigDecimal costPerTriage,
        long scBooked,
        BigDecimal costPerScBooked,
        long scShown,
        BigDecimal costPerScShown,
        long closes,
        BigDecimal costPerClose,
        BigDecimal cashCollected,
        BigDecimal revenue,
        BigDecimal roasCc,
        BigDecimal roasRevenue
) {
}

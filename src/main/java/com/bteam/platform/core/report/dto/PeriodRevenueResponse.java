package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;

public record PeriodRevenueResponse(
        String period,
        BigDecimal revenue,
        long transactionCount
) {
}

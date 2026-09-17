package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        BigDecimal bookingRevenue,
        BigDecimal orderRevenue,
        long transactionCount
) {
}

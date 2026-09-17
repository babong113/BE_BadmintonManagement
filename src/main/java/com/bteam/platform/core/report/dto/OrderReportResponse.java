package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalOrders,
        BigDecimal totalValue,
        BigDecimal completedValue,
        List<StatusCountResponse> byStatus
) {
}

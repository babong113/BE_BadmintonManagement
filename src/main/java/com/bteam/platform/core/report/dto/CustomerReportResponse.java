package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalCustomers,
        BigDecimal averageBookingsPerCustomer,
        List<CustomerActivityResponse> customers
) {
}

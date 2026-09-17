package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CancellationRateResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalBookings,
        long cancelledBookings,
        BigDecimal cancellationRate
) {
}

package com.bteam.platform.core.report.dto;

import java.time.LocalDate;
import java.util.List;

public record BookingReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalBookings,
        List<StatusCountResponse> byStatus,
        List<PeriodCountResponse> daily,
        List<PeriodCountResponse> monthly
) {
}

package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        BigDecimal bookingRevenue,
        BigDecimal orderRevenue,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        BigDecimal cancellationRate,
        long totalOrders,
        long completedOrders,
        long totalCustomers,
        long activeVenues,
        long activeCourts,
        BigDecimal averageRating
) {
}

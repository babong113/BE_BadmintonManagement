package com.bteam.platform.core.report.dto;

import java.time.LocalDate;

public record CustomerActivityResponse(
        Long customerId,
        String customerName,
        String email,
        long bookingCount,
        long completedBookingCount,
        LocalDate lastBookingDate
) {
}

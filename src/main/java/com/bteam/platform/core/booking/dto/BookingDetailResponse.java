package com.bteam.platform.core.booking.dto;

import com.bteam.platform.core.booking.model.BookingDetailStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record BookingDetailResponse(
        Long id,
        Long courtId,
        String courtCode,
        String courtName,
        Long venueId,
        String venueName,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BookingDetailStatus status
) {
}

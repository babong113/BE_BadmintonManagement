package com.bteam.platform.core.booking.dto;

import com.bteam.platform.core.booking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingCode,
        Long venueId,
        String venueName,
        Long customerId,
        String customerName,
        String customerPhone,
        String guestName,
        String guestPhone,
        LocalDate bookingDate,
        BigDecimal totalAmount,
        BookingStatus status,
        String note,
        Long createdBy,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        List<BookingDetailResponse> details
) {
}

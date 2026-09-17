package com.bteam.platform.core.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BookingHistoryResponse(
        Long id,
        String bookingCode,
        LocalDate bookingDate,
        BigDecimal totalAmount,
        String status,
        String note,
        OffsetDateTime createdAt
) {
}

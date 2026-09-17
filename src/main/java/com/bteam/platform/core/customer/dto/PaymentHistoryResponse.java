package com.bteam.platform.core.customer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentHistoryResponse(
        Long id,
        Long bookingId,
        String bookingCode,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        String transactionCode,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}

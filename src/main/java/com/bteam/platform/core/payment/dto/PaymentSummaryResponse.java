package com.bteam.platform.core.payment.dto;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        Long bookingId,
        String bookingCode,
        Long orderId,
        String orderCode,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        BigDecimal refundedAmount,
        BigDecimal remainingAmount
) {
}

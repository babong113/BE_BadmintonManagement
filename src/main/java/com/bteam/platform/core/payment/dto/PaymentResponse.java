package com.bteam.platform.core.payment.dto;

import com.bteam.platform.core.payment.model.PaymentMethod;
import com.bteam.platform.core.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        Long bookingId,
        String bookingCode,
        Long orderId,
        String orderCode,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionCode,
        ZonedDateTime paidAt,
        ZonedDateTime createdAt,
        Long receivedById,
        String receivedByName,
        Long cashierShiftId,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        List<PaymentAllocationResponse> allocations
) {
}

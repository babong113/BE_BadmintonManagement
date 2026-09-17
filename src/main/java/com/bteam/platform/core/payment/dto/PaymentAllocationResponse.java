package com.bteam.platform.core.payment.dto;

import java.math.BigDecimal;

public record PaymentAllocationResponse(
        Long id,
        Long bookingDetailId,
        BigDecimal amount
) {
}

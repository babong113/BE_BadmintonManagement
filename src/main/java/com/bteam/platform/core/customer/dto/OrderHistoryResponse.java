package com.bteam.platform.core.customer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderHistoryResponse(
        Long id,
        String orderCode,
        BigDecimal totalAmount,
        String status,
        OffsetDateTime createdAt
) {
}

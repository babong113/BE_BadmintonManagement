package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;

public record ProductPerformanceResponse(
        Long productId,
        String productName,
        long soldQuantity,
        BigDecimal salesValue,
        long rentedQuantity,
        BigDecimal rentalValue,
        long totalQuantity
) {
}

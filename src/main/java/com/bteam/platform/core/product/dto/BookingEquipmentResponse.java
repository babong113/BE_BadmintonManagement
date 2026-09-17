package com.bteam.platform.core.product.dto;

import java.math.BigDecimal;

public record BookingEquipmentResponse(
        Long id,
        Long bookingDetailId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}

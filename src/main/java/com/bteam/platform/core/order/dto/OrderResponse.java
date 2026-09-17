package com.bteam.platform.core.order.dto;

import com.bteam.platform.core.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        Long venueId,
        String venueName,
        Long customerId,
        String customerName,
        String guestName,
        String guestPhone,
        BigDecimal totalAmount,
        OrderStatus status,
        ZonedDateTime createdAt,
        List<OrderItemResponse> items
) {
}

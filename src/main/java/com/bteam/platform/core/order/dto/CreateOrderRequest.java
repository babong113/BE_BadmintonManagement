package com.bteam.platform.core.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "venueId khong duoc de trong") Long venueId,
        Long customerId,
        @Size(max = 255, message = "Ten khach vang lai khong duoc vuot qua 255 ky tu") String guestName,
        @Size(max = 50, message = "So dien thoai khong duoc vuot qua 50 ky tu") String guestPhone,
        List<@Valid OrderItemRequest> items
) {
}

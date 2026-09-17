package com.bteam.platform.core.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull(message = "So luong khong duoc de trong")
        @PositiveOrZero(message = "So luong khong duoc am")
        Integer quantity
) {
}

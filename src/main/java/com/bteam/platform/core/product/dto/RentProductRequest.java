package com.bteam.platform.core.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RentProductRequest(
        @NotNull(message = "productId khong duoc de trong") Long productId,
        @NotNull(message = "So luong thue khong duoc de trong")
        @Positive(message = "So luong thue phai lon hon 0")
        Integer quantity
) {
}

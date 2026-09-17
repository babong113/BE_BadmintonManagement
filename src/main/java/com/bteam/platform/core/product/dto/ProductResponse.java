package com.bteam.platform.core.product.dto;

import com.bteam.platform.core.product.model.ProductStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ProductResponse(
        Long id,
        String name,
        Integer quantity,
        BigDecimal salePrice,
        BigDecimal rentalPrice,
        boolean isForSale,
        boolean isForRent,
        ProductStatus status,
        ZonedDateTime createdAt
) {
}

package com.bteam.platform.core.product.dto;

import com.bteam.platform.core.product.model.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Ten san pham khong duoc de trong")
        @Size(max = 255, message = "Ten san pham khong duoc vuot qua 255 ky tu")
        String name,
        @DecimalMin(value = "0.00", message = "Gia ban khong duoc am")
        @Digits(integer = 10, fraction = 2, message = "Gia ban toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal salePrice,
        @DecimalMin(value = "0.00", message = "Gia thue khong duoc am")
        @Digits(integer = 10, fraction = 2, message = "Gia thue toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal rentalPrice,
        @NotNull(message = "isForSale khong duoc de trong") Boolean isForSale,
        @NotNull(message = "isForRent khong duoc de trong") Boolean isForRent,
        @NotNull(message = "Trang thai khong duoc de trong") ProductStatus status
) {
}

package com.bteam.platform.core.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AllocationRequest(
        @NotNull(message = "bookingDetailId khong duoc de trong")
        Long bookingDetailId,

        @NotNull(message = "So tien phan bo khong duoc de trong")
        @DecimalMin(value = "0.01", message = "So tien phan bo phai lon hon 0")
        @Digits(integer = 10, fraction = 2, message = "So tien phan bo toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal amount
) {
}

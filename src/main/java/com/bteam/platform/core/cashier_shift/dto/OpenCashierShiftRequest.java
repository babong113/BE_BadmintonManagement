package com.bteam.platform.core.cashier_shift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OpenCashierShiftRequest(
        @NotNull(message = "Co so khong duoc de trong")
        Long venueId,

        @NotNull(message = "Tien dau ca khong duoc de trong")
        @DecimalMin(value = "0.00", message = "Tien dau ca khong duoc am")
        @Digits(integer = 10, fraction = 2, message = "Tien dau ca toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal openingCash,

        @Size(max = 2000, message = "Ghi chu khong duoc vuot qua 2000 ky tu")
        String notes
) {
}

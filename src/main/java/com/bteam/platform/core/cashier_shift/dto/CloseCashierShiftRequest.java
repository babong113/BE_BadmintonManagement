package com.bteam.platform.core.cashier_shift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CloseCashierShiftRequest(
        @NotNull(message = "Tien cuoi ca thuc te khong duoc de trong")
        @DecimalMin(value = "0.00", message = "Tien cuoi ca thuc te khong duoc am")
        @Digits(integer = 10, fraction = 2, message = "Tien cuoi ca toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal actualClosingCash,

        @Size(max = 2000, message = "Ghi chu khong duoc vuot qua 2000 ky tu")
        String notes
) {
}

package com.bteam.platform.core.cashier_shift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CashOutRequest(
        @NotNull(message = "So tien chi khong duoc de trong")
        @DecimalMin(value = "0.01", message = "So tien chi phai lon hon 0")
        @Digits(integer = 10, fraction = 2, message = "So tien chi toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal amount,

        @NotBlank(message = "Ly do chi tien khong duoc de trong")
        @Size(max = 2000, message = "Ly do chi tien khong duoc vuot qua 2000 ky tu")
        String description
) {
}

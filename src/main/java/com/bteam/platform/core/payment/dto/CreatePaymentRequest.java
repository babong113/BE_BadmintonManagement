package com.bteam.platform.core.payment.dto;

import com.bteam.platform.core.payment.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentRequest(
        @NotNull(message = "So tien thanh toan khong duoc de trong")
        @DecimalMin(value = "0.01", message = "So tien thanh toan phai lon hon 0")
        @Digits(integer = 10, fraction = 2, message = "So tien thanh toan toi da 10 chu so nguyen va 2 chu so thap phan")
        BigDecimal amount,

        @NotNull(message = "Phuong thuc thanh toan khong duoc de trong")
        PaymentMethod paymentMethod,

        @Size(max = 255, message = "Ma giao dich khong duoc vuot qua 255 ky tu")
        String transactionCode,

        List<@Valid AllocationRequest> allocations
) {
}

package com.bteam.platform.core.payment.dto;

import com.bteam.platform.core.payment.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePaymentStatusRequest(
        @NotNull(message = "Trang thai thanh toan khong duoc de trong")
        PaymentStatus status,

        @Size(max = 255, message = "Ma giao dich khong duoc vuot qua 255 ky tu")
        String transactionCode
) {
}

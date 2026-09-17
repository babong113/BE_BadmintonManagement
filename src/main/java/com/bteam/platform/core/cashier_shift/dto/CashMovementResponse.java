package com.bteam.platform.core.cashier_shift.dto;

import com.bteam.platform.core.payment.model.CashMovementSourceType;
import com.bteam.platform.core.payment.model.CashMovementType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record CashMovementResponse(
        Long id,
        Long cashierShiftId,
        CashMovementType movementType,
        CashMovementSourceType sourceType,
        BigDecimal amount,
        Long referenceId,
        String description,
        Long createdById,
        String createdByName,
        ZonedDateTime createdAt
) {
}

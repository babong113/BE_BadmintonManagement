package com.bteam.platform.core.cashier_shift.dto;

import com.bteam.platform.core.payment.model.CashierShiftStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record CashierShiftResponse(
        Long id,
        Long staffId,
        String employeeCode,
        String staffName,
        Long venueId,
        String venueName,
        ZonedDateTime openedAt,
        ZonedDateTime closedAt,
        BigDecimal openingCash,
        BigDecimal cashIn,
        BigDecimal cashOut,
        BigDecimal expectedClosingCash,
        BigDecimal actualClosingCash,
        BigDecimal cashDifference,
        CashierShiftStatus status,
        String notes,
        ZonedDateTime createdAt
) {
}

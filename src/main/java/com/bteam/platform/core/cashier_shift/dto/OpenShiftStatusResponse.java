package com.bteam.platform.core.cashier_shift.dto;

public record OpenShiftStatusResponse(
        boolean open,
        Long shiftId,
        Long venueId
) {
}

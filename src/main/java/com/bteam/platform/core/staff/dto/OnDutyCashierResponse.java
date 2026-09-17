package com.bteam.platform.core.staff.dto;

import java.time.OffsetDateTime;

public record OnDutyCashierResponse(
        Long staffId,
        String employeeCode,
        String fullName,
        String email,
        String phoneNumber,
        String position,
        Long shiftId,
        OffsetDateTime openedAt,
        String shiftStatus,
        Long venueId,
        String venueName
) {
}

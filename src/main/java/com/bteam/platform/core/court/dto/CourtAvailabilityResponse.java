package com.bteam.platform.core.court.dto;

import com.bteam.platform.core.venue.model.CourtStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record CourtAvailabilityResponse(
        Long courtId,
        String courtCode,
        String name,
        CourtStatus status,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean available,
        String reason
) {
}

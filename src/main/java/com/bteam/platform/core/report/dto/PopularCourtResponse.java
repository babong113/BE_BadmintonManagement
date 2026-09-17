package com.bteam.platform.core.report.dto;

public record PopularCourtResponse(
        Long courtId,
        String courtName,
        Long venueId,
        String venueName,
        long bookingCount
) {
}

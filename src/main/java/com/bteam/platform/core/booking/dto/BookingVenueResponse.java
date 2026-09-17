package com.bteam.platform.core.booking.dto;

public record BookingVenueResponse(
        Long bookingId,
        String bookingCode,
        Long venueId,
        String venueName
) {
}

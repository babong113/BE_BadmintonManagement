package com.bteam.platform.core.booking.dto;

public record UpdateBookingRequest(
        String guestName,
        String guestPhone,
        String note
) {
}

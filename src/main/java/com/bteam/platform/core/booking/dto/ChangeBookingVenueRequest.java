package com.bteam.platform.core.booking.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeBookingVenueRequest(
        @NotNull(message = "Co so khong duoc de trong")
        Long venueId
) {
}

package com.bteam.platform.core.booking.dto;

import java.math.BigDecimal;

public record AvailableCourtResponse(
        Long courtId,
        Long venueId,
        String venueName,
        String courtCode,
        String courtName,
        BigDecimal pricePerHour
) {
}

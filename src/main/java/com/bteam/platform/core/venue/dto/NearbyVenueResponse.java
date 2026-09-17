package com.bteam.platform.core.venue.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record NearbyVenueResponse(
        Long id,
        String name,
        String address,
        String phoneNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime openingTime,
        LocalTime closingTime,
        double distanceKm
) {
}

package com.bteam.platform.core.venue.dto;

import com.bteam.platform.core.venue.model.VenueStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record VenueResponse(
        Long id,
        String name,
        String address,
        String phoneNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime openingTime,
        LocalTime closingTime,
        VenueStatus status
) {
}

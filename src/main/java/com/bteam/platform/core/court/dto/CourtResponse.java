package com.bteam.platform.core.court.dto;

import com.bteam.platform.core.venue.model.CourtStatus;

import java.math.BigDecimal;

public record CourtResponse(
        Long id,
        Long venueId,
        String venueName,
        String courtCode,
        String name,
        String description,
        BigDecimal pricePerHour,
        CourtStatus status,
        String imageUrl
) {
}

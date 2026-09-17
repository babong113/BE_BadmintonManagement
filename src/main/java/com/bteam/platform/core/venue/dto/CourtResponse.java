package com.bteam.platform.core.venue.dto;

import com.bteam.platform.core.venue.model.CourtStatus;

import java.math.BigDecimal;

public record CourtResponse(
        Long id,
        Long venueId,
        String courtCode,
        String name,
        String description,
        BigDecimal pricePerHour,
        CourtStatus status,
        String imageUrl
) {
}

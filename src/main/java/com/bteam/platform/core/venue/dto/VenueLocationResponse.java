package com.bteam.platform.core.venue.dto;

import java.math.BigDecimal;

public record VenueLocationResponse(
        Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}

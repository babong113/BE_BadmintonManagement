package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;

public record VenueRevenueResponse(
        Long venueId,
        String venueName,
        BigDecimal revenue
) {
}

package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;

public record ReviewPerformanceResponse(
        Long venueId,
        String venueName,
        Long courtId,
        String courtName,
        BigDecimal averageRating,
        long reviewCount
) {
}

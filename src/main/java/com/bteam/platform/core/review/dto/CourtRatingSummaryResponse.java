package com.bteam.platform.core.review.dto;

import java.math.BigDecimal;

public record CourtRatingSummaryResponse(
        Long courtId,
        String courtName,
        BigDecimal averageRating,
        long reviewCount
) {
}

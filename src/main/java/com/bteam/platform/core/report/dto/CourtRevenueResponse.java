package com.bteam.platform.core.report.dto;

import java.math.BigDecimal;

public record CourtRevenueResponse(
        Long courtId,
        String courtName,
        Long venueId,
        String venueName,
        BigDecimal revenue
) {
}

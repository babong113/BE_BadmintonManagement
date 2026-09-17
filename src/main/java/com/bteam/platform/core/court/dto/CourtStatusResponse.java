package com.bteam.platform.core.court.dto;

import com.bteam.platform.core.venue.model.CourtStatus;

public record CourtStatusResponse(
        Long id,
        String courtCode,
        String name,
        CourtStatus status
) {
}

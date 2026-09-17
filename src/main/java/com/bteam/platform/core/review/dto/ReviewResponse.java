package com.bteam.platform.core.review.dto;

import java.time.ZonedDateTime;

public record ReviewResponse(
        Long id,
        Long bookingDetailId,
        Long courtId,
        String courtName,
        Long customerId,
        String customerName,
        Integer rating,
        String comment,
        ZonedDateTime createdAt
) {
}

package com.bteam.platform.core.report.dto;

public record PeakHourResponse(
        int hour,
        String timeRange,
        long bookingCount
) {
}

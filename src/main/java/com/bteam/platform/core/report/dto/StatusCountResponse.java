package com.bteam.platform.core.report.dto;

public record StatusCountResponse(
        String status,
        long total
) {
}
